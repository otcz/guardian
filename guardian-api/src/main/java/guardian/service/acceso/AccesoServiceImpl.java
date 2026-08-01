package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VehiculoResumen;
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
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.repository.spec.AccesoEventoSpecs;
import guardian.security.UsuarioAutenticado;
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
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdVehiculoRepository vehiculoRepository;
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
                .fotoUrl(persona.getFotoUrl())
                .nombreCompleto(persona.getNombreCompleto())
                .documento(persona.getDocumento())
                .casaIdentificador(casa.map(GdCasa::getIdentificador).orElse(null))
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .sentidoSugerido(adentro ? Codigos.SALIDA : Codigos.ENTRADA)
                .vehiculos(vehiculosDe(casa.orElse(null)))
                .payload(request.getPayload())
                .build();
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
            GdInvitacion invitacion = invitacionService.resolver(request.getPayload())
                    .orElseThrow(() -> GuardianException.solicitudInvalida(
                            MensajesGlobales.QR_NO_RECONOCIDO));
            return accesoInvitadoService.registrar(invitacion, request, guardia);
        }

        GdCredencialQr credencial = credencialResuelta.get();
        GdPersona persona = credencial.getPersona();

        if (deOtroConjunto(persona, guardia)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.QR_NO_RECONOCIDO);
        }

        Optional<GdCasa> casa = casaDe(persona);

        // Se revalida todo. Entre el escaneo y el toque del guardia pasan
        // segundos, pero en esos segundos el administrador pudo revocar la
        // credencial — y confiar en el resultado de la verificacion anterior
        // dejaria pasar justo el caso que motivo la revocacion.
        boolean adentro = presenciaService.estaAdentro(persona.getId());
        String motivo = evaluarMotivoDenegacion(credencial, persona, casa.orElse(null));

        if (motivo != null && !adentro) {
            GdAccesoEvento denegado = registrarDenegacion(credencial, casa.orElse(null), motivo,
                    guardia, request.getPuntoAccesoId());
            return fabrica.mapear(denegado);
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

        String modo = request.getModo();
        GdVehiculo vehiculo = resolverVehiculo(request, casa.orElse(null), modo);

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
    public Page<AccesoEventoResponse> buscarEventos(Long conjuntoId, Date desde, Date hasta,
                                                    Long casaId, String resultado,
                                                    Pageable pageable) {

        Specification<GdAccesoEvento> filtro = Specification
                .where(AccesoEventoSpecs.delConjunto(conjuntoId))
                .and(AccesoEventoSpecs.desde(desde))
                .and(AccesoEventoSpecs.hasta(hasta))
                .and(AccesoEventoSpecs.deCasa(casaId))
                .and(AccesoEventoSpecs.conResultado(resultado));

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

        if (!credencial.estaActivo()) {
            return Codigos.MOTIVO_CREDENCIAL_REVOCADA;
        }
        if (credencial.estaVencida(ahora) || credencial.agotoUsos()) {
            return Codigos.MOTIVO_CREDENCIAL_VENCIDA;
        }
        if (!persona.estaActivo()) {
            return Codigos.MOTIVO_PERSONA_INACTIVA;
        }
        // Sin casa vinculada no se deniega: un guardia o un administrador que no
        // vive en el conjunto entra igual. Lo que se bloquea es una casa
        // explicitamente deshabilitada.
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
     */
    private String resolverSentido(RegistrarAccesoRequest request, boolean adentro,
                                   boolean soloSalida, Long personaId) {
        String porPresencia = adentro ? Codigos.SALIDA : Codigos.ENTRADA;

        if (request.getSentido() == null || request.getSentido().equals(porPresencia)) {
            return porPresencia;
        }
        if (soloSalida) {
            throw GuardianException.conflicto(MensajesGlobales.SOLO_SALIDA);
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

    private GdVehiculo resolverVehiculo(RegistrarAccesoRequest request, GdCasa casa, String modo) {
        if (!Codigos.MODO_VEHICULO.equals(modo)) {
            return null;
        }
        if (request.getVehiculoId() == null) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.SELECCIONA_VEHICULO);
        }

        GdVehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        // El vehiculo tiene que ser de la misma casa. Sin este chequeo el
        // frontend podria mandar cualquier id y quedaria registrado que la
        // persona entro en un carro que no es suyo.
        if (casa == null || !vehiculo.getCasa().getId().equals(casa.getId())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.VEHICULO_NO_PERTENECE);
        }
        return vehiculo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escritura de eventos
    // ─────────────────────────────────────────────────────────────────────────

    private GdAccesoEvento registrarDenegacion(GdCredencialQr credencial, GdCasa casa,
                                               String motivo, UsuarioAutenticado guardia,
                                               Long puntoAccesoId) {

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, puntoAccesoId);
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(Codigos.MODO_PEATON);
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

    private Optional<GdCasa> casaDe(GdPersona persona) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa);
    }

    private List<VehiculoResumen> vehiculosDe(GdCasa casa) {
        if (casa == null) {
            return Collections.emptyList();
        }
        return vehiculoRepository
                .findByCasaIdAndActivoOrderByPlacaAsc(casa.getId(), Codigos.SI)
                .stream()
                .map(v -> VehiculoResumen.builder()
                        .id(v.getId())
                        .placa(v.getPlaca())
                        .tipo(v.getTipo())
                        .marca(v.getMarca())
                        .color(v.getColor())
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
                .fotoUrl(persona.getFotoUrl())
                .nombreCompleto(persona.getNombreCompleto())
                .documento(persona.getDocumento())
                .casaIdentificador(casa != null ? casa.getIdentificador() : null)
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .vehiculos(Collections.emptyList())
                .build();
    }

    private String mensajeDe(String motivo) {
        switch (motivo) {
            case Codigos.MOTIVO_CREDENCIAL_REVOCADA:
                return MensajesGlobales.QR_REVOCADO;
            case Codigos.MOTIVO_CREDENCIAL_VENCIDA:
                return MensajesGlobales.QR_VENCIDO;
            case Codigos.MOTIVO_PERSONA_INACTIVA:
                return MensajesGlobales.PERSONA_INACTIVA;
            case Codigos.MOTIVO_CASA_INACTIVA:
                return MensajesGlobales.CASA_INACTIVA;
            default:
                return MensajesGlobales.QR_NO_RECONOCIDO;
        }
    }
}
