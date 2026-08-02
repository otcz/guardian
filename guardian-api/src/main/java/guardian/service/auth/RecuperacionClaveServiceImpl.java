package guardian.service.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.RestablecerClaveRequest;
import guardian.dto.auth.SolicitarCodigoRequest;
import guardian.dto.auth.SolicitudCodigoResponse;
import guardian.entity.auth.GdCodigoRecuperacion;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoRecuperacionRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.EstadoUsuarioService;
import guardian.service.notificacion.CorreoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recuperacion de contrasena por codigo al correo.
 *
 * <p><b>Por que codigo y no enlace.</b> GUARDIAN se usa como aplicacion
 * instalada en el telefono. Un enlace abre el navegador y saca a la persona de
 * la aplicacion — a veces a una sesion distinta — mientras que un codigo la
 * deja donde estaba. Ademas es el gesto que esta audiencia ya conoce del banco
 * y de WhatsApp, y no depende de que el cliente de correo respete el enlace.</p>
 */
@Slf4j
@Service
public class RecuperacionClaveServiceImpl implements RecuperacionClaveService {

    /** Seis digitos: un millon de combinaciones para cinco intentos. */
    private static final int TOPE_CODIGO = 1_000_000;
    private static final String FORMATO_CODIGO = "%06d";
    private static final long MILIS_POR_MINUTO = 60_000L;
    private static final int MAXIMO_DOCUMENTOS_EN_CACHE = 10_000;

    private final GdUsuarioRepository usuarioRepository;
    private final GdCodigoRecuperacionRepository codigoRepository;
    private final CorreoService correoService;
    private final PasswordEncoder passwordEncoder;
    private final EstadoUsuarioService estadoUsuarioService;
    private final ContadorIntentosRecuperacion contadorIntentos;

    private final int minutosVigencia;
    private final int intentosMaximos;
    private final int solicitudesPorHora;

    /**
     * SecureRandom y no Random: este numero ES la llave temporal de una cuenta.
     * Un generador predecible dejaria adivinar el codigo de un vecino sabiendo
     * el momento en que lo pidio.
     */
    private final SecureRandom aleatorio = new SecureRandom();

    /** Solicitudes por documento en la ultima hora. En memoria, como el login. */
    private final Cache<String, AtomicInteger> solicitudes;

    public RecuperacionClaveServiceImpl(
            GdUsuarioRepository usuarioRepository,
            GdCodigoRecuperacionRepository codigoRepository,
            CorreoService correoService,
            PasswordEncoder passwordEncoder,
            EstadoUsuarioService estadoUsuarioService,
            ContadorIntentosRecuperacion contadorIntentos,
            @Value("${guardian.recuperacion.minutos-vigencia}") int minutosVigencia,
            @Value("${guardian.recuperacion.intentos-maximos}") int intentosMaximos,
            @Value("${guardian.recuperacion.solicitudes-por-hora}") int solicitudesPorHora) {

        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.correoService = correoService;
        this.passwordEncoder = passwordEncoder;
        this.estadoUsuarioService = estadoUsuarioService;
        this.contadorIntentos = contadorIntentos;
        this.minutosVigencia = minutosVigencia;
        this.intentosMaximos = intentosMaximos;
        this.solicitudesPorHora = solicitudesPorHora;

        this.solicitudes = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(MAXIMO_DOCUMENTOS_EN_CACHE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Paso 1: pedir el codigo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SolicitudCodigoResponse solicitar(SolicitarCodigoRequest request) {
        String documento = request.getDocumento().trim();

        // Todo lo que sigue puede terminar sin hacer nada, y la respuesta es
        // identica en los cuatro casos: no existe, no tiene cuenta, no tiene
        // correo, o pidio demasiados. Una respuesta distinta en cualquiera de
        // ellos responde "esta cedula vive aca".
        if (excedioSolicitudes(documento)) {
            log.warn("[recuperacion] tope de solicitudes documento={}", documento);
            return respuestaNeutra();
        }

        Optional<GdUsuario> encontrado = usuarioRepository.buscarPorDocumento(documento);
        if (!encontrado.isPresent()) {
            return respuestaNeutra();
        }

        GdUsuario usuario = encontrado.get();
        GdPersona persona = usuario.getPersona();

        // Una cuenta deshabilitada por la administracion no se recupera sola:
        // dejarla recuperar seria darle la vuelta al bloqueo por correo.
        if (!usuario.puedeOperar()) {
            log.warn("[recuperacion] cuenta no operativa usuarioId={}", usuario.getId());
            return respuestaNeutra();
        }
        if (persona.getEmail() == null || persona.getEmail().trim().isEmpty()) {
            log.warn("[recuperacion] persona sin correo personaId={}", persona.getId());
            return respuestaNeutra();
        }

        // Pedir uno nuevo apaga el anterior. Quien lo pide de nuevo suele
        // hacerlo porque sospecha que el primero se le fue a otra persona.
        codigoRepository.revocarVigentesDe(usuario.getId());

        String codigo = generarCodigo();
        GdCodigoRecuperacion fila = new GdCodigoRecuperacion();
        fila.setUsuario(usuario);
        fila.setCodigoHash(passwordEncoder.encode(codigo));
        fila.setFechaVencimiento(new Date(System.currentTimeMillis()
                + minutosVigencia * MILIS_POR_MINUTO));
        fila.setIntentos(0);
        fila.setActivo(Codigos.SI);
        fila.setUsuarioCreador(persona.getDocumento());
        codigoRepository.save(fila);

        correoService.enviarCodigoRecuperacion(persona.getEmail(),
                persona.getNombres(), codigo, minutosVigencia);

        log.info("[recuperacion] codigo emitido usuarioId={}", usuario.getId());
        return respuestaNeutra();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Paso 2: usarlo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void restablecer(RestablecerClaveRequest request) {
        String documento = request.getDocumento().trim();

        GdUsuario usuario = usuarioRepository.buscarPorDocumento(documento)
                .filter(GdUsuario::puedeOperar)
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO));

        GdCodigoRecuperacion fila = codigoRepository
                .findFirstByUsuarioIdOrderByIdDesc(usuario.getId())
                .filter(c -> c.sirve(new Date(), intentosMaximos))
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO));

        if (!passwordEncoder.matches(request.getCodigo().trim(), fila.getCodigoHash())) {
            // En su PROPIA transaccion. Guardarlo aca dentro no servia de nada:
            // la excepcion de la linea siguiente tumbaba la transaccion y con
            // ella el contador, asi que el tope de cinco intentos nunca se
            // alcanzaba y seis digitos quedaban expuestos a fuerza bruta.
            contadorIntentos.fallo(fila.getId());

            log.warn("[recuperacion] codigo incorrecto usuarioId={}", usuario.getId());
            throw GuardianException.solicitudInvalida(
                    MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO);
        }

        // Las mismas reglas que el cambio normal. Recuperar no puede ser la
        // puerta trasera para ponerse una clave que el cambio de clave
        // rechazaria.
        if (request.getClaveNueva().equalsIgnoreCase(documento)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CLAVE_IGUAL_AL_DOCUMENTO);
        }

        usuario.setClaveHash(passwordEncoder.encode(request.getClaveNueva()));
        // La persona acaba de ELEGIR su clave: obligarla a cambiarla otra vez
        // en el siguiente ingreso seria pedirle dos veces lo mismo.
        usuario.setRequiereCambioClave(Codigos.NO);
        usuario.setUsuarioModificador(documento);
        usuarioRepository.save(usuario);

        // Quema el codigo. La fila NO se borra: que alguien recuperara su
        // clave, cuando y desde donde es justo lo que hay que poder mirar
        // despues de una cuenta comprometida.
        fila.setFechaUso(new Date());
        fila.setActivo(Codigos.NO);
        codigoRepository.save(fila);

        // Cualquier sesion abierta con la clave vieja se cae. Si la cuenta
        // estaba comprometida, dejarla viva anularia el punto de recuperarla.
        estadoUsuarioService.invalidar(usuario.getId());

        log.info("[recuperacion] clave restablecida usuarioId={}", usuario.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private SolicitudCodigoResponse respuestaNeutra() {
        return SolicitudCodigoResponse.builder()
                .mensaje(MensajesGlobales.CODIGO_RECUPERACION_ENVIADO)
                .minutosVigencia(minutosVigencia)
                .build();
    }

    private boolean excedioSolicitudes(String documento) {
        AtomicInteger contador = solicitudes.get(documento.toUpperCase(),
                llave -> new AtomicInteger(0));
        return contador.incrementAndGet() > solicitudesPorHora;
    }

    private String generarCodigo() {
        return String.format(FORMATO_CODIGO, aleatorio.nextInt(TOPE_CODIGO));
    }
}
