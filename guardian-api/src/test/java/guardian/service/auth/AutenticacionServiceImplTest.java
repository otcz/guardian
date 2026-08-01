package guardian.service.auth;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.EstadoUsuarioService;
import guardian.security.JwtService;
import guardian.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas del login y del cambio de clave: mensajes que no filtran informacion,
 * orden de los chequeos, freno de intentos y token fresco tras el cambio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutenticacionServiceImplTest {

    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private IntentosLoginService intentosLoginService;
    @Mock private EstadoUsuarioService estadoUsuarioService;

    @InjectMocks
    private AutenticacionServiceImpl servicio;

    private GdUsuario usuario;
    private GdPersona persona;

    @BeforeEach
    void preparar() {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        persona = new GdPersona();
        persona.setId(50L);
        persona.setConjunto(conjunto);
        persona.setDocumento("123");
        persona.setNombres("Ana");
        persona.setApellidos("Diaz");
        persona.setActivo(Codigos.SI);

        usuario = new GdUsuario();
        usuario.setId(9L);
        usuario.setPersona(persona);
        usuario.setRol(Codigos.ROL_RESIDENTE);
        usuario.setClaveHash("$hash");
        usuario.setActivo(Codigos.SI);
        usuario.setRequiereCambioClave(Codigos.NO);

        lenient().when(usuarioRepository.buscarPorDocumento("123"))
                .thenReturn(Optional.of(usuario));
        lenient().when(residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(any(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(jwtService.emitir(any())).thenReturn("token-jwt");
    }

    private LoginRequest loginRequest(String clave) {
        LoginRequest request = new LoginRequest();
        request.setDocumento("123");
        request.setClave(clave);
        return request;
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login correcto emite token y limpia el contador de fallos")
    void loginCorrecto() {
        when(passwordEncoder.matches("clave", "$hash")).thenReturn(true);

        LoginResponse respuesta = servicio.login(loginRequest("clave"));

        assertThat(respuesta.getToken()).isEqualTo("token-jwt");
        verify(intentosLoginService).limpiar("123");
    }

    @Test
    @DisplayName("documento inexistente y clave mala usan el MISMO mensaje")
    void mensajeGenericoNoFiltraExistencia() {
        when(usuarioRepository.buscarPorDocumento("123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.login(loginRequest("x")))
                .hasMessage(MensajesGlobales.CREDENCIALES_INVALIDAS);

        when(usuarioRepository.buscarPorDocumento("123")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("x", "$hash")).thenReturn(false);
        assertThatThrownBy(() -> servicio.login(loginRequest("x")))
                .hasMessage(MensajesGlobales.CREDENCIALES_INVALIDAS);
    }

    @Test
    @DisplayName("cada fallo alimenta el contador de fuerza bruta")
    void fallosAlimentanElContador() {
        when(passwordEncoder.matches("x", "$hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.login(loginRequest("x")))
                .isInstanceOf(GuardianException.class);

        verify(intentosLoginService).registrarFallo("123");
        verify(intentosLoginService, never()).limpiar(anyString());
    }

    @Test
    @DisplayName("usuario inactivo se revela SOLO despues de clave correcta")
    void inactivoSoloTrasClaveCorrecta() {
        usuario.setActivo(Codigos.NO);
        when(passwordEncoder.matches("clave", "$hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.login(loginRequest("clave")))
                .hasMessage(MensajesGlobales.USUARIO_INACTIVO);
    }

    @Test
    @DisplayName("persona inhabilitada bloquea el login aunque el usuario este activo")
    void personaInactivaBloqueaLogin() {
        persona.setActivo(Codigos.NO);
        when(passwordEncoder.matches("clave", "$hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.login(loginRequest("clave")))
                .hasMessage(MensajesGlobales.USUARIO_INACTIVO);
    }

    @Test
    @DisplayName("el token nace con el flag de cambio pendiente")
    void tokenLlevaCambioPendiente() {
        usuario.setRequiereCambioClave(Codigos.SI);
        when(passwordEncoder.matches("123", "$hash")).thenReturn(true);

        LoginResponse respuesta = servicio.login(loginRequest("123"));

        assertThat(respuesta.isRequiereCambioClave()).isTrue();
        verify(jwtService).emitir(org.mockito.ArgumentMatchers
                .argThat(UsuarioAutenticado::isCambioClavePendiente));
    }

    // ── Cambio de clave ──────────────────────────────────────────────────────

    private UsuarioAutenticado autenticado() {
        return new UsuarioAutenticado(9L, 50L, 1L, "123", "Ana Diaz",
                Codigos.ROL_RESIDENTE, true);
    }

    @Test
    @DisplayName("la clave nueva no puede ser el documento, ni cambiando mayusculas")
    void rechazaClaveIgualAlDocumento() {
        usuario.setRequiereCambioClave(Codigos.SI);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123", "$hash")).thenReturn(true);

        CambiarClaveRequest request = new CambiarClaveRequest();
        request.setClaveActual("123");
        request.setClaveNueva("123");

        assertThatThrownBy(() -> servicio.cambiarClave(autenticado(), request))
                .hasMessage(MensajesGlobales.CLAVE_IGUAL_AL_DOCUMENTO);
    }

    @Test
    @DisplayName("cambio exitoso: token nuevo, flag apagado y cache invalidado")
    void cambioExitosoEmiteTokenFresco() {
        usuario.setRequiereCambioClave(Codigos.SI);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123", "$hash")).thenReturn(true);
        when(passwordEncoder.encode("claveNueva9")).thenReturn("$nuevo");

        CambiarClaveRequest request = new CambiarClaveRequest();
        request.setClaveActual("123");
        request.setClaveNueva("claveNueva9");

        LoginResponse respuesta = servicio.cambiarClave(autenticado(), request);

        assertThat(respuesta.getToken()).isEqualTo("token-jwt");
        assertThat(respuesta.isRequiereCambioClave()).isFalse();
        assertThat(usuario.getClaveHash()).isEqualTo("$nuevo");
        assertThat(usuario.debeCambiarClave()).isFalse();
        // Sin invalidar, el token fresco naceria degradado hasta expirar el TTL.
        verify(estadoUsuarioService).invalidar(9L);
    }

    @Test
    @DisplayName("clave actual incorrecta -> rechazado sin tocar nada")
    void rechazaClaveActualIncorrecta() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "$hash")).thenReturn(false);

        CambiarClaveRequest request = new CambiarClaveRequest();
        request.setClaveActual("mala");
        request.setClaveNueva("claveNueva9");

        assertThatThrownBy(() -> servicio.cambiarClave(autenticado(), request))
                .hasMessage(MensajesGlobales.CLAVE_ACTUAL_INCORRECTA);
        assertThat(usuario.getClaveHash()).isEqualTo("$hash");
    }
}
