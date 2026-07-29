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
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.repository.spec.AccesoEventoSpecs;
import guardian.security.UsuarioAutenticado;
import guardian.util.EdadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AccesoServiceImpl implements AccesoService {

    private static final long MILIS_POR_SEGUNDO = 1_000L;

    private final CredencialQrService credencialQrService;
    private final GdCredencialQrRepository credencialRepository;
    private final GdAccesoEventoRepository eventoRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final GdPuntoAccesoRepository puntoAccesoRepository;
    private final GdPersonaRepository personaRepository;
    private final GdInvitacionRepository invitacionRepository;
    private final PresenciaService presenciaService;
    private final InvitacionService invitacionService;

    @Value("${guardian.acceso.segundos-anti-rebote}")
    private long segundosAntiRebote;

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
                return verificarInvitacion(invitacion.get(), request, guardia);
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
        Optional<GdCasa> casa = casaDe(persona);

        String motivo = evaluarMotivoDenegacion(credencial, persona, casa.orElse(null));

        if (motivo != null) {
            registrarDenegacion(credencial, casa.orElse(null), motivo,
                    guardia, request.getPuntoAccesoId());
            return fichaDenegadaConIdentidad(persona, casa.orElse(null), motivo);
        }

        return FichaVerificacionResponse.builder()
                .permitido(true)
                .mensaje(null)
                .fotoUrl(persona.getFotoUrl())
                .nombreCompleto(persona.getNombreCompleto())
                .documento(persona.getDocumento())
                .casaIdentificador(casa.map(GdCasa::getIdentificador).orElse(null))
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .sentidoSugerido(sentidoPorPresencia(persona.getId()))
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
            return registrarInvitacion(invitacion, request, guardia);
        }

        GdCredencialQr credencial = credencialResuelta.get();
        GdPersona persona = credencial.getPersona();
        Optional<GdCasa> casa = casaDe(persona);

        // Se revalida todo. Entre el escaneo y el toque del guardia pasan
        // segundos, pero en esos segundos el administrador pudo revocar la
        // credencial — y confiar en el resultado de la verificacion anterior
        // dejaria pasar justo el caso que motivo la revocacion.
        String motivo = evaluarMotivoDenegacion(credencial, persona, casa.orElse(null));
        if (motivo != null) {
            GdAccesoEvento denegado = registrarDenegacion(credencial, casa.orElse(null), motivo,
                    guardia, request.getPuntoAccesoId());
            return mapear(denegado);
        }

        Optional<GdAccesoEvento> reciente = buscarLecturaReciente(credencial.getId());
        if (reciente.isPresent()) {
            // Doble escaneo. Se devuelve el evento que ya existe en vez de crear
            // uno nuevo: si no, el nerviosismo del guardia generaria una entrada
            // y una salida fantasma que arruinarian el conteo de quien esta
            // adentro.
            log.info("[acceso] doble escaneo ignorado credencialId={} eventoId={}",
                    credencial.getId(), reciente.get().getId());
            return mapear(reciente.get());
        }

        String modo = request.getModo();
        GdVehiculo vehiculo = resolverVehiculo(request, casa.orElse(null), modo);

        // El sentido lo decide el SISTEMA por presencia: quien ya entro no
        // puede volver a entrar, quien ya salio no puede volver a salir. Si el
        // cliente manda un sentido y contradice la presencia real, es una
        // pantalla desactualizada — se rechaza en vez de corromper el conteo.
        String sentido = sentidoPorPresencia(persona.getId());
        if (request.getSentido() != null && !request.getSentido().equals(sentido)) {
            throw GuardianException.conflicto(Codigos.ENTRADA.equals(request.getSentido())
                    ? MensajesGlobales.YA_ADENTRO
                    : MensajesGlobales.YA_AFUERA);
        }

        GdAccesoEvento evento = nuevoEvento(guardia, request.getPuntoAccesoId());
        evento.setSentido(sentido);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_PERMITIDO);
        evento.setCredencial(credencial);
        evento.setPersona(persona);
        evento.setPersonaNombre(persona.getNombreCompleto());
        evento.setPersonaDocumento(persona.getDocumento());

        casa.ifPresent(c -> {
            evento.setCasa(c);
            evento.setCasaIdentificador(c.getIdentificador());
        });

        if (vehiculo != null) {
            evento.setVehiculo(vehiculo);
            evento.setVehiculoPlaca(vehiculo.getPlaca());
        }

        GdAccesoEvento guardado = eventoRepository.save(evento);

        credencial.setUsosRealizados(credencial.getUsosRealizados() + 1);
        credencialRepository.save(credencial);

        log.info("[acceso] registrado personaId={} sentido={} modo={} guardiaId={}",
                persona.getId(), sentido, modo, guardia.getPersonaId());

        return mapear(guardado);
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

        return eventoRepository.findAll(filtro, ordenar(pageable)).map(this::mapear);
    }

    /**
     * Lo mas reciente primero, salvo que el llamante pida otro orden. La
     * bitacora se lee siempre desde el ultimo movimiento hacia atras.
     */
    private Pageable ordenar(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "fechaEvento"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reglas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @return el codigo del motivo por el que se deniega, o {@code null} si el
     *         acceso procede. Un solo lugar con todas las puertas para que
     *         verificar y registrar no puedan divergir.
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

    /** Adentro → lo unico registrable es SALIDA; afuera → ENTRADA. */
    private String sentidoPorPresencia(Long personaId) {
        return presenciaService.estaAdentro(personaId) ? Codigos.SALIDA : Codigos.ENTRADA;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Invitados
    // ─────────────────────────────────────────────────────────────────────────

    private FichaVerificacionResponse verificarInvitacion(GdInvitacion invitacion,
                                                          VerificarQrRequest request,
                                                          UsuarioAutenticado guardia) {
        String motivo = motivoDenegacionInvitacion(invitacion);
        if (motivo != null) {
            registrarDenegacionInvitacion(invitacion, motivo, guardia, request.getPuntoAccesoId());
            return fichaInvitado(invitacion, false, motivo, request.getPayload());
        }
        return fichaInvitado(invitacion, true, null, request.getPayload());
    }

    private AccesoEventoResponse registrarInvitacion(GdInvitacion invitacion,
                                                     RegistrarAccesoRequest request,
                                                     UsuarioAutenticado guardia) {
        // Se revalida igual que con credenciales: entre el escaneo y el toque
        // del guardia el anfitrion pudo revocar la invitacion.
        String motivo = motivoDenegacionInvitacion(invitacion);
        if (motivo != null) {
            return mapear(registrarDenegacionInvitacion(
                    invitacion, motivo, guardia, request.getPuntoAccesoId()));
        }

        Optional<GdAccesoEvento> reciente = buscarLecturaRecienteInvitacion(invitacion.getId());
        if (reciente.isPresent()) {
            log.info("[acceso] doble escaneo de invitacion ignorado invitacionId={} eventoId={}",
                    invitacion.getId(), reciente.get().getId());
            return mapear(reciente.get());
        }

        String sentido = sentidoInvitado(invitacion.getId());
        if (request.getSentido() != null && !request.getSentido().equals(sentido)) {
            throw GuardianException.conflicto(Codigos.ENTRADA.equals(request.getSentido())
                    ? MensajesGlobales.YA_ADENTRO
                    : MensajesGlobales.YA_AFUERA);
        }

        // El invitado no tiene vehiculos registrados en el conjunto: su unica
        // placa valida es la declarada en la invitacion.
        String modo = request.getModo();
        if (Codigos.MODO_VEHICULO.equals(modo) && invitacion.getPlaca() == null) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.INVITADO_SIN_VEHICULO);
        }

        GdAccesoEvento evento = nuevoEvento(guardia, request.getPuntoAccesoId());
        evento.setSentido(sentido);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_PERMITIDO);
        evento.setInvitacion(invitacion);
        evento.setPersonaNombre(invitacion.getNombreInvitado());
        evento.setPersonaDocumento(invitacion.getDocumentoInvitado());
        evento.setCasa(invitacion.getCasa());
        evento.setCasaIdentificador(invitacion.getCasa().getIdentificador());
        if (Codigos.MODO_VEHICULO.equals(modo)) {
            evento.setVehiculoPlaca(invitacion.getPlaca());
        }

        GdAccesoEvento guardado = eventoRepository.save(evento);

        // Solo la ENTRADA consume uso: nadie debe quedar atrapado adentro
        // porque su invitacion "se gasto".
        if (Codigos.ENTRADA.equals(sentido)) {
            invitacion.setUsosRealizados(invitacion.getUsosRealizados() + 1);
            invitacionRepository.save(invitacion);
        }

        log.info("[acceso] invitado registrado invitacionId={} sentido={} usos={}/{} guardiaId={}",
                invitacion.getId(), sentido, invitacion.getUsosRealizados(),
                invitacion.getUsosMaximos(), guardia.getPersonaId());

        return mapear(guardado);
    }

    /**
     * Puertas de la invitacion, en orden de gravedad. Un invitado que ya esta
     * ADENTRO siempre puede salir aunque la invitacion se haya vencido o
     * agotado mientras tanto: retenerlo no protege a nadie.
     */
    private String motivoDenegacionInvitacion(GdInvitacion invitacion) {
        Date ahora = new Date();

        if (!invitacion.estaActivo()) {
            return Codigos.MOTIVO_CREDENCIAL_REVOCADA;
        }
        if (!invitacion.getCasa().estaActivo()) {
            return Codigos.MOTIVO_CASA_INACTIVA;
        }

        boolean adentro = presenciaService.estaAdentroInvitado(invitacion.getId());
        if (adentro) {
            return null;
        }

        if (invitacion.aunNoVigente(ahora)) {
            return Codigos.MOTIVO_INVITACION_NO_VIGENTE;
        }
        if (invitacion.vencida(ahora)) {
            return Codigos.MOTIVO_CREDENCIAL_VENCIDA;
        }
        if (invitacion.agotada()) {
            return Codigos.MOTIVO_INVITACION_AGOTADA;
        }
        return null;
    }

    private String sentidoInvitado(Long invitacionId) {
        return presenciaService.estaAdentroInvitado(invitacionId)
                ? Codigos.SALIDA
                : Codigos.ENTRADA;
    }

    private Optional<GdAccesoEvento> buscarLecturaRecienteInvitacion(Long invitacionId) {
        Date desde = new Date(System.currentTimeMillis() - segundosAntiRebote * MILIS_POR_SEGUNDO);

        return eventoRepository
                .findByInvitacionIdAndFechaEventoAfterOrderByFechaEventoDesc(invitacionId, desde)
                .stream()
                .filter(GdAccesoEvento::fuePermitido)
                .findFirst();
    }

    private GdAccesoEvento registrarDenegacionInvitacion(GdInvitacion invitacion, String motivo,
                                                         UsuarioAutenticado guardia,
                                                         Long puntoAccesoId) {
        GdAccesoEvento evento = nuevoEvento(guardia, puntoAccesoId);
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(Codigos.MODO_PEATON);
        evento.setResultado(Codigos.RESULTADO_DENEGADO);
        evento.setMotivoDenegacion(motivo);
        evento.setInvitacion(invitacion);
        evento.setPersonaNombre(invitacion.getNombreInvitado());
        evento.setPersonaDocumento(invitacion.getDocumentoInvitado());
        evento.setCasa(invitacion.getCasa());
        evento.setCasaIdentificador(invitacion.getCasa().getIdentificador());

        log.info("[acceso] invitado denegado motivo={} invitacionId={} guardiaId={}",
                motivo, invitacion.getId(), guardia.getPersonaId());

        return eventoRepository.save(evento);
    }

    /**
     * Ficha del invitado. Sin foto — el control es el documento fisico contra
     * el declarado — y con el anfitrion visible para que el guardia pueda
     * confirmar con la casa si algo no cuadra.
     */
    private FichaVerificacionResponse fichaInvitado(GdInvitacion invitacion, boolean permitido,
                                                    String motivo, String payload) {
        return FichaVerificacionResponse.builder()
                .permitido(permitido)
                .motivoDenegacion(motivo)
                .mensaje(permitido ? null : mensajeInvitacionDe(motivo))
                .nombreCompleto(invitacion.getNombreInvitado())
                .documento(invitacion.getDocumentoInvitado())
                .casaIdentificador(invitacion.getCasa().getIdentificador())
                .esInvitado(true)
                .anfitrionNombre(invitacion.getAnfitrion().getNombreCompleto())
                .invitacionPlaca(invitacion.getPlaca())
                .sentidoSugerido(permitido ? sentidoInvitado(invitacion.getId()) : null)
                .vehiculos(Collections.emptyList())
                .payload(payload)
                .build();
    }

    private String mensajeInvitacionDe(String motivo) {
        switch (motivo) {
            case Codigos.MOTIVO_INVITACION_NO_VIGENTE:
                return MensajesGlobales.INVITACION_NO_VIGENTE_MSG;
            case Codigos.MOTIVO_INVITACION_AGOTADA:
                return MensajesGlobales.INVITACION_AGOTADA_MSG;
            default:
                return mensajeDe(motivo);
        }
    }

    private Optional<GdAccesoEvento> buscarLecturaReciente(Long credencialId) {
        Date desde = new Date(System.currentTimeMillis() - segundosAntiRebote * MILIS_POR_SEGUNDO);

        return eventoRepository
                .findByCredencialIdAndFechaEventoAfterOrderByFechaEventoDesc(credencialId, desde)
                .stream()
                .filter(GdAccesoEvento::fuePermitido)
                .findFirst();
    }

    private GdVehiculo resolverVehiculo(RegistrarAccesoRequest request, GdCasa casa, String modo) {
        if (!Codigos.MODO_VEHICULO.equals(modo)) {
            return null;
        }
        if (request.getVehiculoId() == null) {
            throw GuardianException.solicitudInvalida("Selecciona el vehiculo.");
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

        GdAccesoEvento evento = nuevoEvento(guardia, puntoAccesoId);
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

        return eventoRepository.save(evento);
    }

    private GdAccesoEvento nuevoEvento(UsuarioAutenticado guardia, Long puntoAccesoId) {
        GdAccesoEvento evento = new GdAccesoEvento();
        evento.setConjuntoId(guardia.getConjuntoId());
        evento.setFechaEvento(new Date());
        evento.setActivo(Codigos.SI);
        evento.setUsuarioCreador(guardia.getDocumento());
        evento.setGuardiaNombre(guardia.getNombreCompleto());

        personaRepository.findById(guardia.getPersonaId()).ifPresent(evento::setGuardia);

        if (puntoAccesoId != null) {
            puntoAccesoRepository.findById(puntoAccesoId).ifPresent(evento::setPuntoAcceso);
        }
        return evento;
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

    private AccesoEventoResponse mapear(GdAccesoEvento evento) {
        return AccesoEventoResponse.builder()
                .id(evento.getId())
                .fechaEvento(evento.getFechaEvento())
                .sentido(evento.getSentido())
                .modo(evento.getModo())
                .resultado(evento.getResultado())
                .motivoDenegacion(evento.getMotivoDenegacion())
                .personaNombre(evento.getPersonaNombre())
                .personaDocumento(evento.getPersonaDocumento())
                .casaIdentificador(evento.getCasaIdentificador())
                .vehiculoPlaca(evento.getVehiculoPlaca())
                .guardiaNombre(evento.getGuardiaNombre())
                .puntoAcceso(evento.getPuntoAcceso() != null
                        ? evento.getPuntoAcceso().getNombre()
                        : null)
                .invitado(evento.getInvitacion() != null)
                .build();
    }
}
