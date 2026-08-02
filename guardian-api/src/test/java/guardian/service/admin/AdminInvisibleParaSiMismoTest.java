package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.UsuarioRequest;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.EstadoUsuarioService;
import guardian.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
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
 * El administrador no se ve a si mismo en el panel.
 *
 * <p>No es cosmetica: verse en la tabla habilita accidentes que no tienen
 * vuelta atras. Un administrador que se inactiva, se bloquea o se cambia el
 * rol queda fuera del sistema y no hay nadie adentro que pueda devolverle el
 * acceso — hay que entrar a la base a mano.</p>
 *
 * <p>Por eso la regla vive en el metodo que RESUELVE la cuenta y no en cada
 * accion: esconder la fila y dejar el endpoint abierto seria una cortina, no
 * una regla, y bastaria un PATCH a mano para saltarsela.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminInvisibleParaSiMismoTest {

    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdPersonaRepository personaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ParametroService parametroService;
    @Mock private EstadoUsuarioService estadoUsuarioService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    /** La cuenta del propio administrador que esta operando. */
    private GdUsuario propia;
    /** La cuenta de un guardia cualquiera de la misma sede. */
    private GdUsuario ajena;

    private UsuarioAutenticado admin;

    @BeforeEach
    void preparar() {
        GdConjunto sede = new GdConjunto();
        sede.setId(1L);

        propia = cuenta(9L, 50L, "ADMIN", "Administrador", Codigos.ROL_ADMIN, sede);
        ajena = cuenta(7L, 40L, "2001", "Luis Mora", Codigos.ROL_GUARDIA, sede);

        admin = new UsuarioAutenticado(9L, 50L, 1L, "ADMIN", "Administrador",
                Codigos.ROL_ADMIN);

        lenient().when(usuarioRepository.listarPorConjunto(1L))
                .thenReturn(Arrays.asList(propia, ajena));
        lenient().when(usuarioRepository.findById(9L)).thenReturn(Optional.of(propia));
        lenient().when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ajena));
        lenient().when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$hash");
    }

    private GdUsuario cuenta(Long usuarioId, Long personaId, String documento,
                             String nombres, String rol, GdConjunto sede) {
        GdPersona persona = new GdPersona();
        persona.setId(personaId);
        persona.setConjunto(sede);
        persona.setDocumento(documento);
        persona.setNombres(nombres);
        persona.setApellidos("");

        GdUsuario usuario = new GdUsuario();
        usuario.setId(usuarioId);
        usuario.setPersona(persona);
        usuario.setRol(rol);
        usuario.setActivo(Codigos.SI);
        return usuario;
    }

    @Test
    @DisplayName("el listado de cuentas no incluye la del que lo pide")
    void elListadoNoSeIncluyeASiMismo() {
        List<?> cuentas = usuarioService.listar(admin);

        assertThat(cuentas).hasSize(1);
        assertThat(usuarioService.listar(admin).get(0).getNombreCompleto())
                .isEqualTo("Luis Mora");
    }

    @Test
    @DisplayName("tampoco se alcanza por id: la propia cuenta responde 404")
    void laPropiaCuentaNoSeAlcanzaPorId() {
        // El mismo 404 que una cuenta de otra sede. Un mensaje distinto
        // confirmaria que existe, y no hay razon para confirmarlo.
        assertThatThrownBy(() -> usuarioService.cambiarEstado(9L, false, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);

        assertThatThrownBy(() -> usuarioService.cambiarRol(9L, Codigos.ROL_RESIDENTE, admin))
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);

        assertThatThrownBy(() -> usuarioService.restablecerClave(9L, admin))
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);

        assertThatThrownBy(() -> usuarioService.asignarClave(9L, "OtraClave9", admin))
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("sobre la cuenta de otro si opera con normalidad")
    void sobreOtrosSiOpera() {
        usuarioService.cambiarEstado(7L, false, admin);
        assertThat(ajena.getActivo()).isEqualTo(Codigos.NO);
    }

    // ── Clave asignada por la administracion ────────────────────────────────

    @Test
    @DisplayName("asignar clave la guarda cifrada y exige el cambio al entrar")
    void asignarClaveExigeCambio() {
        when(passwordEncoder.encode("ClaveDeLaAdmin9")).thenReturn("$asignada");

        usuarioService.asignarClave(7L, "ClaveDeLaAdmin9", admin);

        assertThat(ajena.getClaveHash()).isEqualTo("$asignada");
        // Quien la asigno la conoce: la cuenta no es del dueno hasta que la
        // cambie.
        assertThat(ajena.debeCambiarClave()).isTrue();
    }

    @Test
    @DisplayName("asignar clave corta la sesion abierta del dueno")
    void asignarClaveInvalidaLaSesion() {
        usuarioService.asignarClave(7L, "ClaveDeLaAdmin9", admin);

        // Sin esto el token viejo sigue con autoridad completa hasta que
        // expire el TTL: le cambiamos la clave y seguiria trabajando.
        verify(estadoUsuarioService).invalidar(7L);
    }

    @Test
    @DisplayName("la clave asignada no puede ser el documento del dueno")
    void asignarClaveRechazaElDocumento() {
        assertThatThrownBy(() -> usuarioService.asignarClave(7L, "2001", admin))
                .hasMessage(MensajesGlobales.CLAVE_IGUAL_AL_DOCUMENTO);

        assertThat(ajena.getClaveHash()).isNull();
    }

    @Test
    @DisplayName("restablecer devuelve la clave inicial y corta la sesion abierta")
    void restablecerTambienInvalida() {
        when(passwordEncoder.encode(Codigos.CLAVE_INICIAL)).thenReturn("$inicial");

        usuarioService.restablecerClave(7L, admin);

        assertThat(ajena.getClaveHash()).isEqualTo("$inicial");
        verify(estadoUsuarioService).invalidar(7L);
        assertThat(ajena.debeCambiarClave()).isTrue();
    }

    // ── Clave con la que nace una cuenta ────────────────────────────────────

    @Test
    @DisplayName("una cuenta nueva nace con la clave inicial, NO con el documento")
    void laCuentaNaceConLaClaveInicial() {
        GdPersona persona = new GdPersona();
        persona.setId(60L);
        persona.setDocumento("1099887766");
        GdConjunto sede = new GdConjunto();
        sede.setId(1L);
        persona.setConjunto(sede);

        when(personaRepository.findById(60L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByPersonaId(60L)).thenReturn(false);
        when(passwordEncoder.encode(Codigos.CLAVE_INICIAL)).thenReturn("$inicial");

        UsuarioRequest alta = new UsuarioRequest();
        alta.setPersonaId(60L);
        alta.setRol(Codigos.ROL_RESIDENTE);

        usuarioService.crear(alta, admin);

        ArgumentCaptor<GdUsuario> capturado = ArgumentCaptor.forClass(GdUsuario.class);
        verify(usuarioRepository).save(capturado.capture());

        // La clave que se cifra es la inicial del sistema. Si alguien vuelve a
        // poner el documento, el administrador dictaria "0000" y no entraria.
        verify(passwordEncoder).encode(Codigos.CLAVE_INICIAL);
        verify(passwordEncoder, never()).encode("1099887766");

        assertThat(capturado.getValue().getClaveHash()).isEqualTo("$inicial");
        // HABILITADA desde el alta: activarla en un segundo paso se olvidaba y
        // la persona rebotaba en el login con su clave en la mano.
        assertThat(capturado.getValue().getActivo()).isEqualTo(Codigos.SI);
        // El cambio forzado es lo unico que queda entre la clave inicial y una
        // cuenta de verdad, asi que no se negocia.
        assertThat(capturado.getValue().debeCambiarClave()).isTrue();
    }

    @Test
    @DisplayName("la clave inicial es mas corta que el minimo: nadie puede volver a elegirla")
    void laClaveInicialNoSePuedeReelegir() {
        assertThat(Codigos.CLAVE_INICIAL.length())
                .isLessThan(Codigos.CLAVE_LONGITUD_MINIMA);
    }
}
