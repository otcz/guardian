package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.UsuarioRequest;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Escalada de privilegios: el riesgo numero uno del frente multi-sede.
 *
 * <p>Si un administrador de sede pudiera asignarse SUPER_ADMIN, veria todas
 * las demas sedes con un solo PATCH. Hay TRES caminos hasta ese rol y los tres
 * tienen que estar cerrados — el tercero es el menos obvio: el alta de persona
 * acepta el rol en el mismo request y crea la cuenta de un tiron.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscaladaPrivilegiosTest {

    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdPersonaRepository personaRepository;
    @Mock private ParametroService parametroService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private UsuarioAutenticado admin;

    @BeforeEach
    void preparar() {
        admin = new UsuarioAutenticado(1L, 10L, 1L, "ADMIN", "Admin", Codigos.ROL_ADMIN);

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        GdPersona persona = new GdPersona();
        persona.setId(50L);
        persona.setConjunto(conjunto);
        persona.setDocumento("1001");

        GdUsuario usuario = new GdUsuario();
        usuario.setId(9L);
        usuario.setPersona(persona);
        usuario.setRol(Codigos.ROL_RESIDENTE);

        lenient().when(personaRepository.findById(50L)).thenReturn(Optional.of(persona));
        lenient().when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));
        lenient().when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("camino 1: crear una cuenta con rol SUPER_ADMIN -> rechazado")
    void noSePuedeCrearUnSuperAdmin() {
        UsuarioRequest request = new UsuarioRequest();
        request.setPersonaId(50L);
        request.setRol(Codigos.ROL_SUPER_ADMIN);

        assertThatThrownBy(() -> usuarioService.crear(request, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.ROL_NO_ASIGNABLE);

        // Ni siquiera llega a consultar el catalogo: el rechazo es lo primero.
        verify(parametroService, never()).exigirCodigoValido(anyString(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("camino 2: ascender una cuenta existente a SUPER_ADMIN -> rechazado")
    void noSePuedeAscenderASuperAdmin() {
        assertThatThrownBy(() -> usuarioService.cambiarRol(9L, Codigos.ROL_SUPER_ADMIN, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.ROL_NO_ASIGNABLE);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("el rechazo NO depende de que el rol falte en el catalogo")
    void elBlindajeEsIndependienteDelCatalogo() {
        // Aunque alguien sembrara SUPER_ADMIN en GD_PARAMETRO "para que salga
        // en el combo", el service sigue rechazandolo.
        lenient().doNothing().when(parametroService)
                .exigirCodigoValido(anyString(), anyString());

        assertThatThrownBy(() -> usuarioService.cambiarRol(9L, Codigos.ROL_SUPER_ADMIN, admin))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("un rol normal SI pasa el blindaje y llega a validarse contra el catalogo")
    void losRolesNormalesPasanElBlindaje() {
        usuarioService.cambiarRol(9L, Codigos.ROL_GUARDIA, admin);

        // La prueba de que no se rechazo de entrada: el flujo continuo hasta
        // consultar el catalogo y guardar.
        verify(parametroService).exigirCodigoValido(Codigos.GRUPO_ROL, Codigos.ROL_GUARDIA);
        verify(usuarioRepository).save(any());
    }
}
