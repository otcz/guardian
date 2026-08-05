package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdSolicitudVehiculoRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.EtiquetaCatalogoService;
import guardian.service.admin.ParametroService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que el residente puede y no puede hacer al pedir un vehiculo.
 *
 * <p>Pedir no registra nada — eso es justamente el punto de la aprobacion—,
 * asi que lo que se prueba aca es que la solicitud nazca PENDIENTE, que solo
 * la pida el titular, y que las dos colisiones de placa se corten antes de
 * llegar a la bandeja del administrador.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudVehiculoServiceImplTest {

    @Mock private GdSolicitudVehiculoRepository solicitudRepository;
    @Mock private GdVehiculoRepository vehiculoRepository;
    @Mock private GdPersonaRepository personaRepository;
    @Mock private HogarDelResidente hogar;
    @Mock private ParametroService parametroService;
    @Mock private EtiquetaCatalogoService etiquetaCatalogoService;

    @InjectMocks private SolicitudVehiculoServiceImpl servicio;

    private GdCasa casa;
    private GdPersona titular;
    private UsuarioAutenticado usuario;

    @BeforeEach
    void prepararHogar() {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        casa = new GdCasa();
        casa.setId(10L);
        casa.setIdentificador("CASA-101");
        casa.setConjunto(conjunto);

        titular = new GdPersona();
        titular.setId(50L);
        titular.setDocumento("1073");

        usuario = new UsuarioAutenticado(5L, 50L, 1L, "1073", "Oscar Carrillo",
                Codigos.ROL_RESIDENTE);

        when(hogar.casa(usuario)).thenReturn(casa);
        when(personaRepository.findById(50L)).thenReturn(Optional.of(titular));
        when(solicitudRepository.save(any(GdSolicitudVehiculo.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));
    }

    @Test
    @DisplayName("la solicitud nace PENDIENTE y con la placa normalizada")
    void solicitarDejaTodoEnEspera() {
        when(vehiculoRepository.findByPlaca(anyString())).thenReturn(Optional.empty());
        when(solicitudRepository.findFirstByPlacaAndEstado(anyString(), anyString()))
                .thenReturn(Optional.empty());

        servicio.solicitar(pedido(" abc-123 "), usuario);

        ArgumentCaptor<GdSolicitudVehiculo> guardada =
                ArgumentCaptor.forClass(GdSolicitudVehiculo.class);
        verify(solicitudRepository).save(guardada.capture());

        assertThat(guardada.getValue().getPlaca()).isEqualTo("ABC123");
        assertThat(guardada.getValue().getEstado()).isEqualTo(Codigos.SOLICITUD_PENDIENTE);
        assertThat(guardada.getValue().getCasa()).isSameAs(casa);
        assertThat(guardada.getValue().getSolicitante()).isSameAs(titular);
    }

    @Test
    @DisplayName("un residente que no es titular no puede pedir vehiculos")
    void soloElTitularPide() {
        GuardianException veto = GuardianException.sinPermiso(MensajesGlobales.SOLO_TITULAR_VEHICULOS);
        org.mockito.Mockito.doThrow(veto)
                .when(hogar).exigirTitular(usuario, casa, MensajesGlobales.SOLO_TITULAR_VEHICULOS);

        assertThatThrownBy(() -> servicio.solicitar(pedido("ABC123"), usuario))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLO_TITULAR_VEHICULOS);

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("una placa que ya es de un vehiculo no se puede volver a pedir")
    void placaYaRegistrada() {
        when(vehiculoRepository.findByPlaca("ABC123"))
                .thenReturn(Optional.of(new GdVehiculo()));

        assertThatThrownBy(() -> servicio.solicitar(pedido("ABC123"), usuario))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.PLACA_YA_REGISTRADA);

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("dos casas no pueden tener la misma placa esperando respuesta")
    void placaYaSolicitada() {
        when(vehiculoRepository.findByPlaca("ABC123")).thenReturn(Optional.empty());
        when(solicitudRepository.findFirstByPlacaAndEstado("ABC123", Codigos.SOLICITUD_PENDIENTE))
                .thenReturn(Optional.of(new GdSolicitudVehiculo()));

        assertThatThrownBy(() -> servicio.solicitar(pedido("ABC123"), usuario))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.PLACA_YA_SOLICITADA);

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("descartar una pendiente no la cancela: se rechaza")
    void descartarSoloLoResuelto() {
        GdSolicitudVehiculo pendiente = new GdSolicitudVehiculo();
        pendiente.setId(7L);
        pendiente.setCasa(casa);
        pendiente.setEstado(Codigos.SOLICITUD_PENDIENTE);
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> servicio.descartar(7L, usuario))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLICITUD_NO_RESUELTA);
    }

    @Test
    @DisplayName("descartar una solicitud de otra casa no dice que existe")
    void descartarSoloLoPropio() {
        GdCasa otraCasa = new GdCasa();
        otraCasa.setId(99L);

        GdSolicitudVehiculo ajena = new GdSolicitudVehiculo();
        ajena.setId(7L);
        ajena.setCasa(otraCasa);
        ajena.setEstado(Codigos.SOLICITUD_RECHAZADA);
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> servicio.descartar(7L, usuario))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLICITUD_AJENA);
    }

    @Test
    @DisplayName("descartar apaga la solicitud, no la borra")
    void descartarApaga() {
        GdSolicitudVehiculo rechazada = new GdSolicitudVehiculo();
        rechazada.setId(7L);
        rechazada.setCasa(casa);
        rechazada.setEstado(Codigos.SOLICITUD_RECHAZADA);
        rechazada.setActivo(Codigos.SI);
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(rechazada));

        servicio.descartar(7L, usuario);

        assertThat(rechazada.getActivo()).isEqualTo(Codigos.NO);
        assertThat(rechazada.getEstado()).isEqualTo(Codigos.SOLICITUD_RECHAZADA);
        verify(solicitudRepository, never()).delete(any());
    }

    private VehiculoResidenteRequest pedido(String placa) {
        VehiculoResidenteRequest request = new VehiculoResidenteRequest();
        request.setPlaca(placa);
        request.setTipo("AUTOMOVIL");
        return request;
    }
}
