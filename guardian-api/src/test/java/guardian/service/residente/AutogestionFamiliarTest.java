package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.PersonaResponse;
import guardian.dto.residente.FamiliarRequest;
import guardian.entity.conjunto.GdCasa;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.PersonaRegistrada;
import guardian.service.admin.PersonaService;
import guardian.service.admin.VehiculoService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El alta de un familiar desde "Mi hogar".
 *
 * <p>Lo que se prueba es quien termina con CUENTA. Sin ella la persona existe
 * para la porteria pero no puede abrir la aplicacion, y el login le promete que
 * su PIN es 0000 — una promesa que nadie cumplia.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutogestionFamiliarTest {

    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdCredencialQrRepository credencialRepository;
    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private GdVehiculoRepository vehiculoRepository;
    @Mock private HogarDelResidente hogar;
    @Mock private PersonaService personaService;
    @Mock private VehiculoService vehiculoService;

    @InjectMocks private AutogestionServiceImpl servicio;

    private UsuarioAutenticado titular;

    @BeforeEach
    void prepararHogar() {
        GdCasa casa = new GdCasa();
        casa.setId(10L);
        casa.setIdentificador("CASA-101");

        titular = new UsuarioAutenticado(1L, 50L, 1L, "1074", "Juan Carlos",
                Codigos.ROL_RESIDENTE);

        when(hogar.casa(titular)).thenReturn(casa);

        PersonaResponse creada = PersonaResponse.builder().id(99L).build();
        when(personaService.crear(any(PersonaRequest.class), any()))
                .thenReturn(new PersonaRegistrada(creada, null));
    }

    @Test
    @DisplayName("con correo, el familiar recibe cuenta de RESIDENTE")
    void conCorreoRecibeCuenta() {
        servicio.agregarFamiliar(pedido("esposa@correo.com", "ESPOSA"), titular);

        assertThat(altaEnviada().getRolUsuario()).isEqualTo(Codigos.ROL_RESIDENTE);
        assertThat(altaEnviada().getEmail()).isEqualTo("esposa@correo.com");
    }

    @Test
    @DisplayName("sin correo NO se crea cuenta: es el nino que solo entra al conjunto")
    void sinCorreoNoHayCuenta() {
        servicio.agregarFamiliar(pedido(null, "HIJO"), titular);

        assertThat(altaEnviada().getRolUsuario()).isNull();
    }

    @Test
    @DisplayName("un correo en blanco tampoco crea cuenta")
    void correoEnBlancoTampoco() {
        servicio.agregarFamiliar(pedido("   ", "HIJO"), titular);

        assertThat(altaEnviada().getRolUsuario()).isNull();
    }

    /**
     * El rol lo fija el service y NUNCA viene del request. Si algun dia alguien
     * le agrega {@code rolUsuario} a FamiliarRequest, un titular podria crearse
     * un ADMIN desde el celular — y este test se cae antes.
     */
    @Test
    @DisplayName("el titular no puede elegir el rol del familiar")
    void elRolNoSeNegocia() {
        servicio.agregarFamiliar(pedido("otro@correo.com", "OTRO"), titular);

        assertThat(altaEnviada().getRolUsuario()).isEqualTo(Codigos.ROL_RESIDENTE);
    }

    private PersonaRequest altaEnviada() {
        ArgumentCaptor<PersonaRequest> captor = ArgumentCaptor.forClass(PersonaRequest.class);
        verify(personaService).crear(captor.capture(), any());
        return captor.getValue();
    }

    private FamiliarRequest pedido(String email, String parentesco) {
        FamiliarRequest request = new FamiliarRequest();
        request.setTipoDocumento("CC");
        request.setDocumento("1076");
        request.setNombres("Raul");
        request.setApellidos("Carrillo");
        request.setEmail(email);
        request.setParentesco(parentesco);
        return request;
    }
}
