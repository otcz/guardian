package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.entity.conjunto.GdConjunto;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.acceso.CredencialQrService;
import guardian.service.foto.FotoStorageService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * El tercer camino a SUPER_ADMIN, y el menos obvio: el alta de persona acepta
 * el rol en el MISMO request y crea la cuenta de un tiron. Quien blinde solo
 * UsuarioServiceImpl deja esta puerta abierta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonaEscaladaTest {

    @Mock private GdPersonaRepository personaRepository;
    @Mock private GdConjuntoRepository conjuntoRepository;
    @Mock private GdCasaRepository casaRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdCredencialQrRepository credencialRepository;
    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdAccesoEventoRepository eventoRepository;
    @Mock private GdInvitacionRepository invitacionRepository;
    @Mock private CredencialQrService credencialQrService;
    @Mock private ParametroService parametroService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FotoStorageService fotoStorageService;

    @InjectMocks
    private PersonaServiceImpl servicio;

    private UsuarioAutenticado admin;

    @BeforeEach
    void preparar() {
        admin = new UsuarioAutenticado(1L, 10L, 1L, "ADMIN", "Admin", Codigos.ROL_ADMIN);

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        lenient().when(personaRepository.findByDocumento(any())).thenReturn(Optional.empty());
        lenient().when(personaRepository.findByTelefono(any())).thenReturn(Optional.empty());
        lenient().when(conjuntoRepository.findById(1L)).thenReturn(Optional.of(conjunto));
        lenient().when(personaRepository.save(any())).thenAnswer(inv -> {
            guardian.entity.persona.GdPersona p = inv.getArgument(0);
            p.setId(77L);
            return p;
        });
    }

    private PersonaRequest alta(String rolUsuario) {
        PersonaRequest request = new PersonaRequest();
        request.setDocumento("2002");
        request.setNombres("Colado");
        request.setApellidos("Por Atras");
        request.setRolUsuario(rolUsuario);
        // Toda cuenta exige correo: es por donde su duena recupera la clave.
        request.setEmail("colado@correo.com");
        return request;
    }

    @Test
    @DisplayName("el rol se valida ANTES que el correo")
    void elRolSeValidaAntesQueElCorreo() {
        // Una regla de completitud no puede colarse delante de una de
        // seguridad: si respondiera "escribe el correo", el segundo intento
        // —ya con correo— encontraria abierta la puerta que este chequeo
        // cierra.
        PersonaRequest sinCorreo = alta(Codigos.ROL_SUPER_ADMIN);
        sinCorreo.setEmail(null);

        assertThatThrownBy(() -> servicio.crear(sinCorreo, admin))
                .hasMessage(MensajesGlobales.ROL_NO_ASIGNABLE);
    }

    @Test
    @DisplayName("una cuenta sin correo no se crea: nace sin forma de recuperarse")
    void laCuentaExigeCorreo() {
        PersonaRequest sinCorreo = alta(Codigos.ROL_RESIDENTE);
        sinCorreo.setEmail("   ");

        assertThatThrownBy(() -> servicio.crear(sinCorreo, admin))
                .hasMessage(MensajesGlobales.CORREO_REQUERIDO_CON_CUENTA);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("alta de persona con rolUsuario=SUPER_ADMIN -> rechazada")
    void noSeCuelaPorElAltaDePersona() {
        assertThatThrownBy(() -> servicio.crear(alta(Codigos.ROL_SUPER_ADMIN), admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.ROL_NO_ASIGNABLE);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("alta con un rol normal si crea la cuenta")
    void elAltaConRolNormalSiCreaCuenta() {
        servicio.crear(alta(Codigos.ROL_RESIDENTE), admin);

        verify(parametroService).exigirCodigoValido(Codigos.GRUPO_ROL, Codigos.ROL_RESIDENTE);
        verify(usuarioRepository).save(any());
    }
}
