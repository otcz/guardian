package guardian.service.auth;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.RestablecerClaveRequest;
import guardian.dto.auth.SolicitarCodigoRequest;
import guardian.dto.auth.SolicitudCodigoResponse;
import guardian.entity.auth.GdCodigoRecuperacion;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoRecuperacionRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.EstadoUsuarioService;
import guardian.service.notificacion.CorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recuperacion de clave. Lo que se prueba aca no es el camino feliz: es que la
 * pantalla no se vuelva un verificador de cedulas ni una forma de entrar sin
 * saber la clave.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecuperacionClaveServiceImplTest {

    private static final int MINUTOS = 10;
    private static final int INTENTOS_MAXIMOS = 5;
    private static final int SOLICITUDES_POR_HORA = 3;

    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdCodigoRecuperacionRepository codigoRepository;
    @Mock private CorreoService correoService;
    @Mock private EstadoUsuarioService estadoUsuarioService;
    @Mock private ContadorIntentosRecuperacion contadorIntentos;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private RecuperacionClaveServiceImpl servicio;
    private GdUsuario usuario;
    private GdPersona persona;

    @BeforeEach
    void preparar() {
        servicio = new RecuperacionClaveServiceImpl(usuarioRepository, codigoRepository,
                correoService, passwordEncoder, estadoUsuarioService, contadorIntentos,
                MINUTOS, INTENTOS_MAXIMOS, SOLICITUDES_POR_HORA);

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        persona = new GdPersona();
        persona.setId(50L);
        persona.setConjunto(conjunto);
        persona.setNombres("Ana");
        persona.setApellidos("Diaz");
        persona.setDocumento("1001");
        persona.setEmail("ana@correo.com");

        usuario = new GdUsuario();
        usuario.setId(7L);
        usuario.setPersona(persona);
        usuario.setRol(Codigos.ROL_RESIDENTE);
        usuario.setClaveHash(passwordEncoder.encode("laVieja123"));
        usuario.setActivo(Codigos.SI);
        usuario.setBloqueado(Codigos.NO);
        usuario.setRequiereCambioClave(Codigos.NO);

        lenient().when(usuarioRepository.buscarPorDocumento("1001"))
                .thenReturn(Optional.of(usuario));
        lenient().when(codigoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── No filtrar quien existe ──────────────────────────────────────────────

    @Test
    @DisplayName("un documento inexistente responde EXACTAMENTE lo mismo que uno real")
    void noRevelaSiElDocumentoExiste() {
        // Es toda la regla: esta pantalla esta abierta sin sesion, y una
        // respuesta distinta la convertiria en un verificador de que cedulas
        // viven en el conjunto.
        when(usuarioRepository.buscarPorDocumento("9999")).thenReturn(Optional.empty());

        SolicitudCodigoResponse real = servicio.solicitar(solicitud("1001"));
        SolicitudCodigoResponse inventado = servicio.solicitar(solicitud("9999"));

        assertThat(inventado).isEqualTo(real);
        assertThat(real.getMensaje()).isEqualTo(MensajesGlobales.CODIGO_RECUPERACION_ENVIADO);
    }

    @Test
    @DisplayName("una persona sin correo tampoco se distingue de una inexistente")
    void noRevelaSiFaltaElCorreo() {
        persona.setEmail(null);

        SolicitudCodigoResponse respuesta = servicio.solicitar(solicitud("1001"));

        assertThat(respuesta.getMensaje()).isEqualTo(MensajesGlobales.CODIGO_RECUPERACION_ENVIADO);
        verify(correoService, never()).enviarCodigoRecuperacion(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("una cuenta deshabilitada NO se recupera por correo")
    void cuentaDeshabilitadaNoSeRecupera() {
        // Si no, el correo seria la vuelta por detras al bloqueo que puso la
        // administracion.
        usuario.setBloqueado(Codigos.SI);

        servicio.solicitar(solicitud("1001"));

        verify(correoService, never()).enviarCodigoRecuperacion(any(), any(), any(), anyInt());
    }

    // ── El codigo ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("el codigo se guarda HASHEADO, nunca en claro")
    void elCodigoSeGuardaHasheado() {
        // Mientras vive es equivalente a la clave: quien lo lea de la base
        // puede tomarse la cuenta.
        servicio.solicitar(solicitud("1001"));

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigoRecuperacion(eq("ana@correo.com"), eq("Ana"),
                enviado.capture(), eq(MINUTOS));

        ArgumentCaptor<GdCodigoRecuperacion> guardado =
                ArgumentCaptor.forClass(GdCodigoRecuperacion.class);
        verify(codigoRepository).save(guardado.capture());

        assertThat(enviado.getValue()).hasSize(6).containsOnlyDigits();
        assertThat(guardado.getValue().getCodigoHash()).isNotEqualTo(enviado.getValue());
        assertThat(passwordEncoder.matches(enviado.getValue(),
                guardado.getValue().getCodigoHash())).isTrue();
    }

    @Test
    @DisplayName("pedir uno nuevo apaga el anterior")
    void pedirOtroApagaElAnterior() {
        // Quien pide otro suele hacerlo porque sospecha que el primero se le
        // fue a la persona equivocada.
        servicio.solicitar(solicitud("1001"));

        verify(codigoRepository).revocarVigentesDe(7L);
    }

    @Test
    @DisplayName("se corta el envio repetido al mismo documento")
    void topeDeSolicitudes() {
        for (int i = 0; i < SOLICITUDES_POR_HORA; i++) {
            servicio.solicitar(solicitud("1001"));
        }
        servicio.solicitar(solicitud("1001"));

        // El cuarto no envia, pero responde igual que los tres primeros: el
        // tope tampoco puede delatar nada.
        verify(correoService, org.mockito.Mockito.times(SOLICITUDES_POR_HORA))
                .enviarCodigoRecuperacion(any(), any(), any(), anyInt());
    }

    // ── Usarlo ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("con el codigo correcto la clave cambia y las sesiones se caen")
    void restableceYCortaSesiones() {
        prepararCodigoVigente("123456");

        servicio.restablecer(restablecer("123456", "5847"));

        assertThat(passwordEncoder.matches("5847", usuario.getClaveHash())).isTrue();
        // No se le vuelve a pedir cambiarla: acaba de elegirla.
        assertThat(usuario.getRequiereCambioClave()).isEqualTo(Codigos.NO);
        // Si la cuenta estaba comprometida, dejar viva la sesion del intruso
        // anularia el punto de recuperarla.
        verify(estadoUsuarioService).invalidar(7L);
    }

    @Test
    @DisplayName("el codigo se quema: no sirve dos veces")
    void elCodigoEsDeUnSoloUso() {
        GdCodigoRecuperacion fila = prepararCodigoVigente("123456");

        servicio.restablecer(restablecer("123456", "5847"));

        assertThat(fila.estaUsado()).isTrue();
        // La fila NO se borra: que alguien recuperara su clave y cuando es
        // justo lo que hay que poder mirar despues.
        verify(codigoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("el intento fallido se cuenta FUERA de la transaccion que revienta")
    void cuentaLosIntentosFallidos() {
        // La version anterior hacia fila.setIntentos(+1) y save() dentro del
        // mismo metodo transaccional que despues lanza. La excepcion revertia
        // el contador, el tope de cinco no se alcanzaba nunca y seis digitos
        // quedaban expuestos a fuerza bruta. Se descubrio probando contra la
        // base real: el test viejo pasaba porque un mock no revierte nada.
        //
        // Por eso se verifica la LLAMADA al bean de transaccion propia y no el
        // estado del objeto en memoria, que es justo lo que enmascaraba el bug.
        GdCodigoRecuperacion fila = prepararCodigoVigente("123456");
        fila.setId(99L);

        assertThatThrownBy(() -> servicio.restablecer(restablecer("000000", "5847")))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO);

        verify(contadorIntentos).fallo(99L);
        assertThat(passwordEncoder.matches("5847", usuario.getClaveHash())).isFalse();
    }

    @Test
    @DisplayName("agotados los intentos, ni el codigo correcto sirve")
    void codigoQuemadoPorIntentos() {
        GdCodigoRecuperacion fila = prepararCodigoVigente("123456");
        fila.setIntentos(INTENTOS_MAXIMOS);

        assertThatThrownBy(() -> servicio.restablecer(restablecer("123456", "5847")))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("un codigo vencido no sirve")
    void codigoVencido() {
        GdCodigoRecuperacion fila = prepararCodigoVigente("123456");
        fila.setFechaVencimiento(new Date(System.currentTimeMillis() - 1000));

        assertThatThrownBy(() -> servicio.restablecer(restablecer("123456", "5847")))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("codigo malo, vencido, usado y documento inexistente dan el MISMO mensaje")
    void unSoloMensajeParaTodosLosFallos() {
        // Distinguirlos le diria a quien prueba codigos cual de las cuatro
        // cosas acerto.
        when(usuarioRepository.buscarPorDocumento("9999")).thenReturn(Optional.empty());
        GdCodigoRecuperacion fila = prepararCodigoVigente("123456");

        assertThatThrownBy(() -> servicio.restablecer(restablecer("000000", "5847")))
                .hasMessage(MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO);

        fila.setFechaUso(new Date());
        assertThatThrownBy(() -> servicio.restablecer(restablecer("123456", "5847")))
                .hasMessage(MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO);

        RestablecerClaveRequest ajeno = restablecer("123456", "5847");
        ajeno.setDocumento("9999");
        assertThatThrownBy(() -> servicio.restablecer(ajeno))
                .hasMessage(MensajesGlobales.CODIGO_RECUPERACION_NO_VALIDO);
    }

    @Test
    @DisplayName("recuperar no es la puerta trasera para poner un PIN trivial")
    void noPermiteLaClaveIgualAlDocumento() {
        // El cambio normal lo rechaza; si aca pasara, bastaria con recuperar
        // para dejar la cuenta tan expuesta como antes.
        prepararCodigoVigente("123456");

        assertThatThrownBy(() -> servicio.restablecer(restablecer("123456", "1234")))
                .hasMessage(MensajesGlobales.PIN_TRIVIAL);
        assertThatThrownBy(() -> servicio.restablecer(restablecer("123456", "1001")))
                .hasMessage(MensajesGlobales.PIN_SALE_DEL_DOCUMENTO);
    }

    @Test
    @DisplayName("el codigo de una cuenta no sirve para otra")
    void elCodigoEsDeSuDueno() {
        prepararCodigoVigente("123456");
        // Otro usuario, sin ningun codigo emitido.
        GdUsuario vecino = new GdUsuario();
        vecino.setId(8L);
        vecino.setPersona(persona);
        vecino.setActivo(Codigos.SI);
        vecino.setBloqueado(Codigos.NO);
        when(usuarioRepository.buscarPorDocumento("2002")).thenReturn(Optional.of(vecino));
        when(codigoRepository.findFirstByUsuarioIdOrderByIdDesc(8L)).thenReturn(Optional.empty());

        RestablecerClaveRequest delVecino = restablecer("123456", "5847");
        delVecino.setDocumento("2002");

        assertThatThrownBy(() -> servicio.restablecer(delVecino))
                .isInstanceOf(GuardianException.class);
    }

    // ── Ayudas ───────────────────────────────────────────────────────────────

    private GdCodigoRecuperacion prepararCodigoVigente(String codigo) {
        GdCodigoRecuperacion fila = new GdCodigoRecuperacion();
        fila.setUsuario(usuario);
        fila.setCodigoHash(passwordEncoder.encode(codigo));
        fila.setFechaVencimiento(new Date(System.currentTimeMillis() + 600_000));
        fila.setIntentos(0);
        fila.setActivo(Codigos.SI);
        when(codigoRepository.findFirstByUsuarioIdOrderByIdDesc(7L)).thenReturn(Optional.of(fila));
        return fila;
    }

    private SolicitarCodigoRequest solicitud(String documento) {
        SolicitarCodigoRequest request = new SolicitarCodigoRequest();
        request.setDocumento(documento);
        return request;
    }

    private RestablecerClaveRequest restablecer(String codigo, String claveNueva) {
        RestablecerClaveRequest request = new RestablecerClaveRequest();
        request.setDocumento("1001");
        request.setCodigo(codigo);
        request.setClaveNueva(claveNueva);
        return request;
    }
}
