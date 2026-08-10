package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdCodigoHogarRepository;
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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Eliminar una persona no puede reventar contra las FK de
 * {@code GD_CODIGO_HOGAR}: quien fue titular o quien se unio con un codigo
 * tiene que poder borrarse igual que cualquiera, y el codigo sobrevive como
 * historial con la referencia en null (ver GdCodigoHogar.titular).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonaEliminarTest {

    @Mock private GdPersonaRepository personaRepository;
    @Mock private GdConjuntoRepository conjuntoRepository;
    @Mock private GdCasaRepository casaRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdCredencialQrRepository credencialRepository;
    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdAccesoEventoRepository eventoRepository;
    @Mock private GdInvitacionRepository invitacionRepository;
    @Mock private GdCodigoHogarRepository codigoHogarRepository;
    @Mock private CredencialQrService credencialQrService;
    @Mock private ParametroService parametroService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FotoStorageService fotoStorageService;

    @InjectMocks
    private PersonaServiceImpl servicio;

    private UsuarioAutenticado admin;

    @BeforeEach
    void preparar() {
        admin = new UsuarioAutenticado(1L, 1L, 1L, "ADMIN", "Admin", Codigos.ROL_SUPER_ADMIN);
    }

    @Test
    @DisplayName("eliminar desvincula al titular y a quien se registro con su codigo")
    void eliminarDesvinculaCodigosDeHogar() {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        GdPersona persona = new GdPersona();
        persona.setId(30L);
        persona.setDocumento("999");
        persona.setConjunto(conjunto);
        when(personaRepository.findById(30L)).thenReturn(Optional.of(persona));

        servicio.eliminar(30L, admin);

        verify(codigoHogarRepository).desvincularTitular(30L);
        verify(codigoHogarRepository).desvincularPersonaRegistrada(30L);
        verify(personaRepository).delete(persona);
    }
}
