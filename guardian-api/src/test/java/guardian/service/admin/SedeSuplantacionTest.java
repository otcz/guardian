package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.LoginResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.exception.GuardianException;
import guardian.repository.GdConjuntoRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Entrar y salir de una sede. La sede viaja en el token, nunca como parametro
 * del cliente, y por eso cada puerta de entrada se prueba aca.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SedeSuplantacionTest {

    @Mock private GdConjuntoRepository conjuntoRepository;
    @Mock private guardian.repository.GdPuntoAccesoRepository puntoAccesoRepository;
    @Mock private guardian.repository.GdPersonaRepository personaRepository;
    @Mock private guardian.repository.GdUsuarioRepository usuarioRepository;
    @Mock private guardian.repository.GdCasaRepository casaRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private SedeServiceImpl servicio;

    private GdConjunto sede;
    private UsuarioAutenticado superAdmin;

    @BeforeEach
    void preparar() {
        sede = new GdConjunto();
        sede.setId(7L);
        sede.setNombre("Torres del Parque");
        sede.setEsPlataforma(Codigos.NO);
        sede.setActivo(Codigos.SI);
        sede.setBloqueado(Codigos.NO);

        superAdmin = new UsuarioAutenticado(4L, 5L, null, "SUPERADMIN",
                "Super Administrador", Codigos.ROL_SUPER_ADMIN);

        lenient().when(conjuntoRepository.findById(7L)).thenReturn(Optional.of(sede));
        lenient().when(jwtService.emitir(any())).thenReturn("token-de-sede");
    }

    @Test
    @DisplayName("entrar a una sede devuelve sesion nueva marcada como suplantada")
    void entrarMarcaLaSuplantacion() {
        LoginResponse respuesta = servicio.entrar(7L, superAdmin);

        assertThat(respuesta.getToken()).isEqualTo("token-de-sede");
        assertThat(respuesta.getUsuario().getSedeId()).isEqualTo(7L);
        assertThat(respuesta.getUsuario().getSedeNombre()).isEqualTo("Torres del Parque");
        assertThat(respuesta.getUsuario().isSedeSuplantada()).isTrue();
    }

    @Test
    @DisplayName("no se entra a una sede DESACTIVADA: el token naceria muerto")
    void noSeEntraASedeDesactivada() {
        sede.setActivo(Codigos.NO);

        assertThatThrownBy(() -> servicio.entrar(7L, superAdmin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SEDE_NO_OPERATIVA);
    }

    @Test
    @DisplayName("tampoco a una sede BLOQUEADA")
    void noSeEntraASedeBloqueada() {
        sede.setBloqueado(Codigos.SI);

        assertThatThrownBy(() -> servicio.entrar(7L, superAdmin))
                .hasMessage(MensajesGlobales.SEDE_NO_OPERATIVA);
    }

    @Test
    @DisplayName("la fila de plataforma NO es una sede a la que se pueda entrar")
    void noSeEntraALaFilaDePlataforma() {
        sede.setEsPlataforma(Codigos.SI);

        assertThatThrownBy(() -> servicio.entrar(7L, superAdmin))
                .hasMessage(MensajesGlobales.SEDE_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("salir devuelve una sesion SIN sede: el token viejo deja de mandar")
    void salirLimpiaLaSede() {
        UsuarioAutenticado dentro = new UsuarioAutenticado(4L, 5L, 7L, "SUPERADMIN",
                "Super Administrador", Codigos.ROL_SUPER_ADMIN, false, true);

        LoginResponse respuesta = servicio.salir(dentro);

        assertThat(respuesta.getUsuario().getSedeId()).isNull();
        assertThat(respuesta.getUsuario().getSedeNombre()).isNull();
        assertThat(respuesta.getUsuario().isSedeSuplantada()).isFalse();
    }

    @Test
    @DisplayName("la plataforma no se lista como una sede mas")
    void listarExcluyeLaPlataforma() {
        when(conjuntoRepository.findByEsPlataformaOrderByNombreAsc(Codigos.NO))
                .thenReturn(java.util.Collections.singletonList(sede));

        assertThat(servicio.listar()).hasSize(1);
        // El filtro es por es_plataforma='N': si alguien lo cambia por findAll,
        // la fila tecnica aparece en el panel y alguien intenta entrar a ella.
        org.mockito.Mockito.verify(conjuntoRepository)
                .findByEsPlataformaOrderByNombreAsc(Codigos.NO);
    }
}
