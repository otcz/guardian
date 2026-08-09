package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.acceso.FiltroEventos;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VehiculoResumen;
import guardian.dto.acceso.VerificarDocumentoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.repository.spec.AccesoEventoSpecs;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.EtiquetaCatalogoService;
import guardian.util.EdadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Flujo de porteria para RESIDENTES y consulta de la bitacora. El flujo de
 * invitados vive en {@link AccesoInvitadoService}; lo comun a ambos — creacion
 * de eventos, anti-rebote, mapeo — en {@link AccesoEventoFabrica}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccesoServiceImpl implements AccesoService {

    /** Tope de pagina de la bitacora: un tamano arbitrario seria un DoS gratis. */
    private static final int TAMANO_MAXIMO_PAGINA = 200;

    private final CredencialQrService credencialQrService;
    private final GdCredencialQrRepository credencialRepository;
    private final GdPersonaRepository personaRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final EtiquetaCatalogoService etiquetaCatalogoService;
    private final PresenciaService presenciaService;
    private final InvitacionService invitacionService;
    private final AccesoInvitadoService accesoInvitadoService;
    private final AccesoEventoFabrica fabrica;
    private final GdAccesoEventoRepository eventoRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Verificacion
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FichaVerificacionResponse verificar(VerificarQrRequest request,
                                               UsuarioAutenticado guardia) {

        Optional<GdCredencialQr> encontrada = credencialQrService.resolver(request.getPayload());

        if (!encontrada.isPresent()) {
            // ¿Es una invitacion? El prefijo del payload distingue los dos
            // mundos y cada resolver valida su propia firma.
            Optional<GdInvitacion> invitacion = invitacionService.resolver(request.getPayload());
            if (invitacion.isPresent()) {
                return accesoInvitadoService.verificar(invitacion.get(), request, guardia);
            }

            // Ni siquiera sabemos de quien es el codigo, pero el intento se
            // registra igual: un QR falsificado insistiendo en la porteria es
            // exactamente lo que el administrador necesita ver.
            registrarDenegacion(null, null, Codigos.MOTIVO_FIRMA_INVALIDA,
                    guardia, request.getPuntoAccesoId());
            return fichaDenegada(Codigos.MOTIVO_FIRMA_INVALIDA, MensajesGlobales.QR_NO_RECONOCIDO);
        }

        GdCredencialQr credencial = encontrada.get();
        GdPersona persona = credencial.getPersona();

        if (deOtroConjunto(persona, guardia)) {
            registrarDenegacion(null, null, Codigos.MOTIVO_FIRMA_INVALIDA,
                    guardia, request.getPuntoAccesoId());
            return fichaDenegada(Codigos.MOTIVO_FIRMA_INVALIDA, MensajesGlobales.QR_NO_RECONOCIDO);
        }

        Optional<GdCasa> casa = casaDe(persona);
        boolean adentro = presenciaService.estaAdentro(persona.getId());
        String motivo = evaluarMotivoDenegacion(credencial, persona, casa.orElse(null));

        if (motivo != null && !adentro) {
            registrarDenegacion(credencial, casa.orElse(null), motivo,
                    guardia, request.getPuntoAccesoId());
            return fichaDenegadaConIdentidad(persona, casa.orElse(null), motivo);
        }

        // motivo != null pero esta ADENTRO: la salida se permite siempre —
        // retener a alguien dentro del conjunto no protege a nadie. La ficha
        // avisa que es SOLO salida y el registrar rechaza cualquier intento
        // de convertirla en entrada.
        boolean soloSalida = motivo != null;

        return FichaVerificacionResponse.builder()
                .permitido(true)
                .mensaje(soloSalida ? MensajesGlobales.SOLO_SALIDA : null)
                // Tambien en la salida: el guardia le dice a la persona por que
                // no va a poder volver a entrar, en vez de mandarla a preguntar.
                .motivoBloqueo(soloSalida
                        ? motivoBloqueoDe(motivo, persona, casa.orElse(null)) : null)
                .fotoUrl(persona.getFotoUrl())
                .nombreCompleto(persona.getNombreCompleto())
                .tipoDocumento(persona.getTipoDocumento())
                .documento(persona.getDocumento())
                .casaIdentificador(casa.map(GdCasa::getIdentificador).orElse(null))
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .sentidoSugerido(adentro ? Codigos.SALIDA : Codigos.ENTRADA)
                .vehiculos(vehiculosDe(casa.orElse(null)))
                .payload(request.getPayload())
                .build();
    }

    /**
     * Identificacion por documento.
     *
     * <p>Resuelve a la persona y despues DELEGA en {@link #verificar}: las
     * reglas de bloqueo, casa, presencia y registro del intento son las mismas
     * y no pueden divergir. Reconstruir aca esa evaluacion seria tener dos
     * porterias con criterios que se separan al primer cambio.</p>
     */
    @Override
    @Transactional
    public FichaVerificacionResponse verificarPorDocumento(VerificarDocumentoRequest request,
                                                           UsuarioAutenticado guardia) {
        String documento = request.getDocumento().trim();

        Optional<GdPersona> encontrada = personaRepository
                .findByConjuntoIdAndDocumento(guardia.getConjuntoId(), documento);

        if (!encontrada.isPresent()) {
            // Antes de darlo por desconocido: puede ser un INVITADO que llega
            // sin el codigo —bateria muerta, link que no le llego— y entrega la
            // cedula. Ese documento no es un dato suelto: es el que el
            // anfitrion declaro al invitarlo. Sin esta rama, la porteria le
            // decia "no hay nadie registrado" a alguien que si estaba invitado.
            Optional<GdInvitacion> invitado = invitacionService
                    .buscarPorDocumento(guardia.getConjuntoId(), documento);
            if (invitado.isPresent()) {
                return verificarInvitacion(invitado.get(), request.getPuntoAccesoId(), guardia);
            }

            // Queda registrado igual: alguien probando cedulas en la porteria
            // es exactamente lo que el administrador necesita poder ver.
            registrarDenegacion(null, null, Codigos.MOTIVO_FIRMA_INVALIDA,
                    guardia, request.getPuntoAccesoId());
            return fichaDenegada(Codigos.MOTIVO_FIRMA_INVALIDA,
                    MensajesGlobales.DOCUMENTO_NO_ENCONTRADO);
        }

        GdPersona persona = encontrada.get();

        Optional<GdCredencialQr> credencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI);

        if (!credencial.isPresent()) {
            // Existe en el sistema pero sin credencial vigente: casi siempre le
            // falta la foto. Se muestra QUIEN es —el guardia ya lo tiene
            // enfrente— y por que no pasa, en vez de un "no encontrado" que lo
            // dejaria buscando de nuevo.
            registrarDenegacion(null, casaDe(persona).orElse(null),
                    Codigos.MOTIVO_CREDENCIAL_REVOCADA, guardia, request.getPuntoAccesoId());
            return fichaDenegadaConIdentidad(persona, casaDe(persona).orElse(null),
                    Codigos.MOTIVO_CREDENCIAL_REVOCADA);
        }

        VerificarQrRequest comoQr = new VerificarQrRequest();
        comoQr.setPayload(credencialQrService.construirPayload(credencial.get()));
        comoQr.setPuntoAccesoId(request.getPuntoAccesoId());
        return verificar(comoQr, guardia);
    }

    /**
     * Un invitado identificado por documento entra por la MISMA puerta que si
     * hubiera mostrado el codigo.
     *
     * <p>Se le arma el payload de su invitacion y se delega: vigencia,
     * revocacion, casa y presencia las decide {@link AccesoInvitadoService} en
     * un solo sitio. Ademas la ficha vuelve con ese payload, que es lo que el
     * guardia necesita para poder registrar el paso.</p>
     */
    private FichaVerificacionResponse verificarInvitacion(GdInvitacion invitacion,
                                                          Long puntoAccesoId,
                                                          UsuarioAutenticado guardia) {
        VerificarQrRequest comoQr = new VerificarQrRequest();
        comoQr.setPayload(invitacionService.construirPayload(invitacion));
        comoQr.setPuntoAccesoId(puntoAccesoId);
        return accesoInvitadoService.verificar(invitacion, comoQr, guardia);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registro
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AccesoEventoResponse registrar(RegistrarAccesoRequest request,
                                          UsuarioAutenticado guardia) {

        Optional<GdCredencialQr> credencialResuelta =
                credencialQrService.resolver(request.getPayload());

        if (!credencialResuelta.isPresent()) {
            Optional<GdInvitacion> invitacion = invitacionService.resolver(request.getPayload());
            if (invitacion.isPresent()) {
                return accesoInvitadoService.registrar(invitacion.get(), request, guardia);
            }
            // Antes lanzaba y no dejaba rastro. Un QR falsificado insistiendo en
            // la porteria es exactamente lo que hay que poder mirar despues, y
            // verificar ya lo registraba: registrar no tenia por que ser la
            // puerta silenciosa de las dos.
            return fabrica.mapear(registrarDenegacion(null, null,
                    Codigos.MOTIVO_FIRMA_INVALIDA, guardia, request));
        }

        GdCredencialQr credencial = credencialResuelta.get();
        GdPersona persona = credencial.getPersona();

        if (deOtroConjunto(persona, guardia)) {
            // Para esta porteria el codigo no existe, pero el intento si: que
            // un QR de otra sede aparezca aca es justo lo que el operador de la
            // plataforma necesita ver.
            return fabrica.mapear(registrarDenegacion(null, null,
                    Codigos.MOTIVO_FIRMA_INVALIDA, guardia, request));
        }

        Optional<GdCasa> casa = casaDe(persona);

        // Se revalida todo. Entre el escaneo y el toque del guardia pasan
        // segundos, pero en esos segundos el administrador pudo revocar la
        // credencial — y confiar en el resultado de la verificacion anterior
        // dejaria pasar justo el caso que motivo la revocacion.
        boolean adentro = presenciaService.estaAdentro(persona.getId());
        String motivo = evaluarMotivoDenegacion(credencial, persona, casa.orElse(null));

        if (motivo != null && !adentro) {
            return fabrica.mapear(registrarDenegacion(credencial, casa.orElse(null), motivo,
                    guardia, request));
        }

        Optional<GdAccesoEvento> reciente = fabrica.lecturaReciente(credencial.getId());
        if (reciente.isPresent()) {
            // Doble escaneo. Se devuelve el evento que ya existe en vez de crear
            // uno nuevo: si no, el nerviosismo del guardia generaria una entrada
            // y una salida fantasma que arruinarian el conteo de quien esta
            // adentro.
            log.info("[acceso] doble escaneo ignorado credencialId={} eventoId={}",
                    credencial.getId(), reciente.get().getId());
            return fabrica.mapear(reciente.get());
        }

        boolean soloSalida = motivo != null;
        String sentido = resolverSentido(request, adentro, soloSalida, persona.getId());

        // resolverSentido devuelve null cuando el guardia insiste en ENTRAR a
        // alguien que solo puede salir. Es una entrada NEGADA, no un error de
        // pantalla: va a la bitacora con el motivo que la dejo en solo-salida.
        if (sentido == null) {
            return fabrica.mapear(registrarDenegacion(credencial, casa.orElse(null),
                    Codigos.MOTIVO_ENTRADA_TRAS_SALIDA, guardia, request));
        }

        String modo = request.getModo();
        VehiculoResuelto delVehiculo = resolverVehiculo(request, casa.orElse(null), modo);

        if (delVehiculo.motivo != null) {
            return fabrica.mapear(registrarDenegacionDeVehiculo(credencial, casa.orElse(null),
                    delVehiculo, sentido, guardia, request));
        }
        GdVehiculo vehiculo = delVehiculo.vehiculo;

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, request.getPuntoAccesoId());
        evento.setSentido(sentido);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_PERMITIDO);
        evento.setCredencial(credencial);
        evento.setPersona(persona);
        evento.setPersonaNombre(persona.getNombreCompleto());
        evento.setPersonaDocumento(persona.getDocumento());

        if (soloSalida) {
            evento.setObservaciones("Salida permitida con credencial en estado " + motivo);
        }

        casa.ifPresent(c -> {
            evento.setCasa(c);
            evento.setCasaIdentificador(c.getIdentificador());
        });

        if (vehiculo != null) {
            evento.setVehiculo(vehiculo);
            evento.setVehiculoPlaca(vehiculo.getPlaca());
        }

        GdAccesoEvento guardado = fabrica.guardar(evento);

        credencial.setUsosRealizados(credencial.getUsosRealizados() + 1);
        credencialRepository.save(credencial);

        log.info("[acceso] registrado personaId={} sentido={} modo={} guardiaId={}",
                persona.getId(), sentido, modo, guardia.getPersonaId());

        return fabrica.mapear(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccesoEventoResponse> buscarEventos(Long conjuntoId, FiltroEventos filtros,
                                                    Pageable pageable) {

        // El filtro del conjunto va SIEMPRE y no es negociable: sin el, un
        // administrador veria por quien entro en la sede del vecino.
        Specification<GdAccesoEvento> filtro = Specification
                .where(AccesoEventoSpecs.delConjunto(conjuntoId))
                .and(AccesoEventoSpecs.desde(filtros.getDesde()))
                .and(AccesoEventoSpecs.hasta(filtros.getHasta()))
                .and(AccesoEventoSpecs.deCasa(filtros.getCasaId()))
                .and(AccesoEventoSpecs.conValoresDe("resultado", filtros.getResultados()))
                .and(AccesoEventoSpecs.conValoresDe("sentido", filtros.getSentidos()))
                .and(AccesoEventoSpecs.conValoresDe("modo", filtros.getModos()))
                .and(AccesoEventoSpecs.conValoresDe("motivoDenegacion", filtros.getMotivos()))
                .and(AccesoEventoSpecs.enPorterias(filtros.getPorteriaIds()))
                .and(AccesoEventoSpecs.queDiga(filtros.getTexto()));

        return eventoRepository.findAll(filtro, ordenar(pageable)).map(fabrica::mapear);
    }

    /**
     * Lo mas reciente primero, salvo que el llamante pida otro orden, y con el
     * tamano de pagina acotado. La bitacora se lee siempre desde el ultimo
     * movimiento hacia atras.
     */
    private Pageable ordenar(Pageable pageable) {
        int tamano = Math.min(pageable.getPageSize(), TAMANO_MAXIMO_PAGINA);

        Sort orden = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "fechaEvento");

        return PageRequest.of(pageable.getPageNumber(), tamano, orden);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reglas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @return el codigo del motivo por el que se deniega, o {@code null} si el
     *         acceso procede. Un solo lugar con todas las puertas para que
     *         verificar y registrar no puedan divergir. La excepcion de
     *         "adentro siempre sale" NO vive aca: la aplican los llamantes,
     *         que son quienes conocen la presencia.
     */
    private String evaluarMotivoDenegacion(GdCredencialQr credencial,
                                           GdPersona persona,
                                           GdCasa casa) {
        Date ahora = new Date();

        if (!credencial.puedeOperar()) {
            return Codigos.MOTIVO_CREDENCIAL_REVOCADA;
        }
        if (credencial.estaVencida(ahora) || credencial.agotoUsos()) {
            return Codigos.MOTIVO_CREDENCIAL_VENCIDA;
        }
        // El bloqueo de la administracion se reporta aparte de "inactiva": al
        // guardia no le sirve el mismo mensaje para "la familia lo apago" que
        // para "la administracion lo bloqueo".
        if (persona.estaBloqueado()) {
            return Codigos.MOTIVO_PERSONA_BLOQUEADA;
        }
        if (!persona.estaActivo()) {
            return Codigos.MOTIVO_PERSONA_INACTIVA;
        }
        // Sin casa vinculada no se deniega: un guardia o un administrador que no
        // vive en el conjunto entra igual. Lo que se bloquea es una casa
        // explicitamente deshabilitada.
        if (casa != null && casa.estaBloqueado()) {
            return Codigos.MOTIVO_CASA_BLOQUEADA;
        }
        if (casa != null && !casa.estaActivo()) {
            return Codigos.MOTIVO_CASA_INACTIVA;
        }
        return null;
    }

    /**
     * El sentido lo decide la presencia. Un sentido explicito que la
     * contradiga solo se acepta con {@code corregirSentido} — la correccion
     * consciente del guardia (CONTEXT.md seccion 4) — y nunca cuando la
     * credencial esta en estado de solo-salida.
     *
     * @return el sentido, o {@code null} si se pidio ENTRAR a alguien que solo
     *         puede salir. Ese caso vuelve como null y no como excepcion porque
     *         es una negacion que tiene que quedar en la bitacora, y una
     *         excepcion tumbaria la transaccion que la escribe.
     */
    private String resolverSentido(RegistrarAccesoRequest request, boolean adentro,
                                   boolean soloSalida, Long personaId) {
        String porPresencia = adentro ? Codigos.SALIDA : Codigos.ENTRADA;

        if (request.getSentido() == null || request.getSentido().equals(porPresencia)) {
            return porPresencia;
        }
        if (soloSalida) {
            log.warn("[acceso] reingreso negado personaId={} credencial en solo-salida", personaId);
            return null;
        }
        if (Boolean.TRUE.equals(request.getCorregirSentido())) {
            log.warn("[acceso] sentido corregido por el guardia personaId={} de={} a={}",
                    personaId, porPresencia, request.getSentido());
            return request.getSentido();
        }
        throw GuardianException.conflicto(Codigos.ENTRADA.equals(request.getSentido())
                ? MensajesGlobales.YA_ADENTRO
                : MensajesGlobales.YA_AFUERA);
    }

    private boolean deOtroConjunto(GdPersona persona, UsuarioAutenticado guardia) {
        boolean ajena = !persona.getConjunto().getId().equals(guardia.getConjuntoId());
        if (ajena) {
            // Frontera de tenant: para esta porteria el codigo no existe.
            log.warn("[acceso] credencial de otro conjunto personaId={} guardiaConjunto={}",
                    persona.getId(), guardia.getConjuntoId());
        }
        return ajena;
    }

    /**
     * El vehiculo del intento, o el motivo por el que no sale.
     *
     * <p>Devuelve el motivo en vez de lanzarlo porque estas negaciones tienen
     * que quedar en la bitacora, y una excepcion tumbaria la transaccion que
     * las escribe. El vehiculo viaja incluso cuando hay motivo: la placa del
     * intento es justamente el dato que se quiere leer despues.</p>
     */
    private VehiculoResuelto resolverVehiculo(RegistrarAccesoRequest request, GdCasa casa,
                                              String modo) {
        if (!Codigos.MODO_VEHICULO.equals(modo)) {
            return VehiculoResuelto.ninguno();
        }
        // Sin id no hubo intento de sacar NADA: es una peticion mal formada del
        // cliente, no algo que pasara en la porteria. Esa si se rechaza seca.
        if (request.getVehiculoId() == null) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.SELECCIONA_VEHICULO);
        }

        GdVehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        // El vehiculo es de la CASA, no de una persona: cualquier miembro del
        // nucleo puede salir en cualquier vehiculo de su casa. Lo que se
        // impide es usar el de otra casa mandando un id cualquiera — y eso,
        // que antes moria en un 400 mudo, ahora queda escrito.
        if (casa == null || !vehiculo.getCasa().getId().equals(casa.getId())) {
            return VehiculoResuelto.negado(vehiculo, Codigos.MOTIVO_VEHICULO_AJENO);
        }
        // Las dos llaves, y con motivos distintos. Uno deshabilitado por la
        // administracion no sale aunque la familia lo tenga activo, y uno que
        // la familia desactivo tampoco — pero al guardia le tienen que quedar
        // claras las dos causas: una la levanta la administracion y la otra el
        // titular desde su celular.
        if (vehiculo.estaBloqueado()) {
            return VehiculoResuelto.negado(vehiculo, Codigos.MOTIVO_VEHICULO_BLOQUEADO);
        }
        if (!vehiculo.estaActivo()) {
            return VehiculoResuelto.negado(vehiculo, Codigos.MOTIVO_VEHICULO_INACTIVO);
        }
        return VehiculoResuelto.permitido(vehiculo);
    }

    /** Vehiculo del intento y, si no sale, por que. */
    private static final class VehiculoResuelto {
        private final GdVehiculo vehiculo;
        private final String motivo;

        private VehiculoResuelto(GdVehiculo vehiculo, String motivo) {
            this.vehiculo = vehiculo;
            this.motivo = motivo;
        }

        static VehiculoResuelto ninguno() {
            return new VehiculoResuelto(null, null);
        }

        static VehiculoResuelto permitido(GdVehiculo vehiculo) {
            return new VehiculoResuelto(vehiculo, null);
        }

        static VehiculoResuelto negado(GdVehiculo vehiculo, String motivo) {
            return new VehiculoResuelto(vehiculo, motivo);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escritura de eventos
    // ─────────────────────────────────────────────────────────────────────────

    /** Denegacion desde la VERIFICACION, donde no hay peticion de registro. */
    private GdAccesoEvento registrarDenegacion(GdCredencialQr credencial, GdCasa casa,
                                               String motivo, UsuarioAutenticado guardia,
                                               Long puntoAccesoId) {
        return escribirDenegacion(credencial, casa, motivo, guardia, puntoAccesoId,
                Codigos.MODO_PEATON);
    }

    /**
     * Denegacion desde el REGISTRO. Conserva el modo que el guardia toco: leer
     * despues "se nego a pie" cuando el intento fue en carro convierte la
     * bitacora en un relato distinto del que ocurrio.
     */
    private GdAccesoEvento registrarDenegacion(GdCredencialQr credencial, GdCasa casa,
                                               String motivo, UsuarioAutenticado guardia,
                                               RegistrarAccesoRequest request) {
        String modo = request.getModo() != null ? request.getModo() : Codigos.MODO_PEATON;
        return escribirDenegacion(credencial, casa, motivo, guardia,
                request.getPuntoAccesoId(), modo);
    }

    /**
     * Denegacion de un vehiculo. Deja la placa y el sentido del intento: sin la
     * placa, la bitacora diria que se nego "un vehiculo" sin decir cual, y esa
     * fila no sirve para responder la pregunta que motivo mirarla.
     */
    private GdAccesoEvento registrarDenegacionDeVehiculo(GdCredencialQr credencial, GdCasa casa,
                                                         VehiculoResuelto delVehiculo,
                                                         String sentido,
                                                         UsuarioAutenticado guardia,
                                                         RegistrarAccesoRequest request) {
        GdAccesoEvento evento = construirDenegacion(credencial, casa, delVehiculo.motivo,
                guardia, request.getPuntoAccesoId(), Codigos.MODO_VEHICULO);
        evento.setSentido(sentido);

        if (delVehiculo.vehiculo != null) {
            evento.setVehiculoPlaca(delVehiculo.vehiculo.getPlaca());
            // La FK solo cuando el carro ES de esta casa. El ajeno se nombra
            // por su placa y nada mas: enlazarlo colgaria de la bitacora de una
            // casa un vehiculo que pertenece a otra.
            if (!Codigos.MOTIVO_VEHICULO_AJENO.equals(delVehiculo.motivo)) {
                evento.setVehiculo(delVehiculo.vehiculo);
            }
        }
        return fabrica.guardar(evento);
    }

    private GdAccesoEvento escribirDenegacion(GdCredencialQr credencial, GdCasa casa,
                                              String motivo, UsuarioAutenticado guardia,
                                              Long puntoAccesoId, String modo) {
        return fabrica.guardar(construirDenegacion(credencial, casa, motivo, guardia,
                puntoAccesoId, modo));
    }

    private GdAccesoEvento construirDenegacion(GdCredencialQr credencial, GdCasa casa,
                                               String motivo, UsuarioAutenticado guardia,
                                               Long puntoAccesoId, String modo) {

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, puntoAccesoId);
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_DENEGADO);
        evento.setMotivoDenegacion(motivo);

        if (credencial != null) {
            GdPersona persona = credencial.getPersona();
            evento.setCredencial(credencial);
            evento.setPersona(persona);
            evento.setPersonaNombre(persona.getNombreCompleto());
            evento.setPersonaDocumento(persona.getDocumento());
        }
        if (casa != null) {
            evento.setCasa(casa);
            evento.setCasaIdentificador(casa.getIdentificador());
        }

        log.info("[acceso] denegado motivo={} credencialId={} guardiaId={}",
                motivo, credencial != null ? credencial.getId() : null, guardia.getPersonaId());

        return fabrica.guardar(evento);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapeo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * La casa para EVALUAR el ingreso se resuelve SIN mirar si el vinculo esta
     * activo.
     *
     * <p>Si se filtrara por vinculo activo, apagarlo dejaria a la persona "sin
     * casa" — y sin casa no se deniega. Con el administrador bloqueando una
     * casa completa, bastaria con que alguien apagara su propio vinculo desde
     * el celular para evaporar el bloqueo de toda la familia.</p>
     */
    private Optional<GdCasa> casaDe(GdPersona persona) {
        return residenteCasaRepository
                .findFirstByPersonaIdOrderByIdAsc(persona.getId())
                .map(GdResidenteCasa::getCasa);
    }

    /**
     * Solo los vehiculos que el registro va a aceptar. Ofrecerle al guardia una
     * placa que despues se rechaza es peor que no ofrecerla: toca, falla, y la
     * fila crece.
     */
    private List<VehiculoResumen> vehiculosDe(GdCasa casa) {
        if (casa == null) {
            return Collections.emptyList();
        }
        return vehiculoRepository
                .operativosDeLaCasa(casa.getId())
                .stream()
                .map(v -> VehiculoResumen.builder()
                        .id(v.getId())
                        .placa(v.getPlaca())
                        .tipo(etiquetaCatalogoService
                                .etiqueta(Codigos.GRUPO_TIPO_VEHICULO, v.getTipo()))
                        .marca(etiquetaCatalogoService
                                .etiqueta(Codigos.GRUPO_MARCA_VEHICULO, v.getMarca()))
                        .color(etiquetaCatalogoService
                                .etiqueta(Codigos.GRUPO_COLOR_VEHICULO, v.getColor()))
                        .build())
                .collect(Collectors.toList());
    }

    private FichaVerificacionResponse fichaDenegada(String motivo, String mensaje) {
        return FichaVerificacionResponse.builder()
                .permitido(false)
                .motivoDenegacion(motivo)
                .mensaje(mensaje)
                .vehiculos(Collections.emptyList())
                .build();
    }

    /**
     * Denegacion en la que si sabemos quien es. Se muestran nombre, foto y casa
     * a proposito: el guardia tiene que poder explicarle a la persona por que no
     * pasa, y "el sistema dice que no" no le sirve a nadie en la porteria.
     */
    private FichaVerificacionResponse fichaDenegadaConIdentidad(GdPersona persona,
                                                                GdCasa casa,
                                                                String motivo) {
        return FichaVerificacionResponse.builder()
                .permitido(false)
                .motivoDenegacion(motivo)
                .mensaje(mensajeDe(motivo))
                .motivoBloqueo(motivoBloqueoDe(motivo, persona, casa))
                .fotoUrl(persona.getFotoUrl())
                .nombreCompleto(persona.getNombreCompleto())
                .tipoDocumento(persona.getTipoDocumento())
                .documento(persona.getDocumento())
                .casaIdentificador(casa != null ? casa.getIdentificador() : null)
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .vehiculos(Collections.emptyList())
                .build();
    }

    /**
     * El texto que escribio la administracion al deshabilitar, sacado de QUIEN
     * esta bloqueado de verdad.
     *
     * <p>Persona y casa se bloquean por separado y con motivos distintos —"no
     * autorizado" no es lo mismo que "la casa esta en mora"—, asi que se lee
     * el de la entidad que produjo la denegacion y no el primero que haya.</p>
     */
    private String motivoBloqueoDe(String motivo, GdPersona persona, GdCasa casa) {
        if (Codigos.MOTIVO_PERSONA_BLOQUEADA.equals(motivo)) {
            return persona != null ? persona.getMotivoBloqueo() : null;
        }
        if (Codigos.MOTIVO_CASA_BLOQUEADA.equals(motivo)) {
            return casa != null ? casa.getMotivoBloqueo() : null;
        }
        // El resto —credencial vencida, firma invalida, inactiva— no nace de un
        // bloqueo administrativo y no tiene motivo escrito por nadie.
        return null;
    }

    private String mensajeDe(String motivo) {
        switch (motivo) {
            case Codigos.MOTIVO_CREDENCIAL_REVOCADA:
                return MensajesGlobales.QR_REVOCADO;
            case Codigos.MOTIVO_CREDENCIAL_VENCIDA:
                return MensajesGlobales.QR_VENCIDO;
            case Codigos.MOTIVO_PERSONA_INACTIVA:
                return MensajesGlobales.PERSONA_INACTIVA;
            case Codigos.MOTIVO_PERSONA_BLOQUEADA:
                return MensajesGlobales.PERSONA_BLOQUEADA;
            case Codigos.MOTIVO_CASA_INACTIVA:
                return MensajesGlobales.CASA_INACTIVA;
            case Codigos.MOTIVO_CASA_BLOQUEADA:
                return MensajesGlobales.CASA_BLOQUEADA;
            default:
                return MensajesGlobales.QR_NO_RECONOCIDO;
        }
    }
}
