package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdSolicitudVehiculoRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La autorizacion del vehiculo.
 *
 * <p>Aprobar aca es lo unico que crea la placa en el sistema, y una placa
 * creada es un carro al que la porteria le abre sin volver a preguntar. Por eso
 * se prueba que el alta pase por el service de vehiculos —que es el que sabe de
 * placa unica y de sede— y que una solicitud de otra sede ni siquiera exista
 * para este administrador.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudVehiculoAdminServiceImplTest {

    @Mock private GdSolicitudVehiculoRepository solicitudRepository;
    @Mock private VehiculoService vehiculoService;
    @Mock private EtiquetaCatalogoService etiquetaCatalogoService;

    @InjectMocks private SolicitudVehiculoAdminServiceImpl servicio;

    private UsuarioAutenticado administrador;
    private GdSolicitudVehiculo solicitud;

    @BeforeEach
    void prepararBandeja() {
        administrador = new UsuarioAutenticado(9L, 90L, 1L, "1073", "Admin del conjunto",
                Codigos.ROL_ADMIN);

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        GdCasa casa = new GdCasa();
        casa.setId(10L);
        casa.setIdentificador("CASA-101");
        casa.setConjunto(conjunto);

        GdPersona quienPidio = new GdPersona();
        quienPidio.setId(50L);
        quienPidio.setDocumento("1074");
        quienPidio.setNombres("Oscar");
        quienPidio.setApellidos("Carrillo");

        solicitud = new GdSolicitudVehiculo();
        solicitud.setId(7L);
        solicitud.setCasa(casa);
        solicitud.setSolicitante(quienPidio);
        solicitud.setPlaca("ABC123");
        solicitud.setTipo("AUTOMOVIL");
        solicitud.setEstado(Codigos.SOLICITUD_PENDIENTE);

        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(GdSolicitudVehiculo.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));
        when(vehiculoService.crear(any(VehiculoRequest.class), eq(administrador)))
                .thenReturn(VehiculoResponse.builder().id(300L).placa("ABC123").build());
    }

    @Test
    @DisplayName("aprobar crea el vehiculo por el service de vehiculos")
    void aprobarCreaElVehiculo() {
        servicio.aprobar(7L, administrador);

        ArgumentCaptor<VehiculoRequest> alta = ArgumentCaptor.forClass(VehiculoRequest.class);
        verify(vehiculoService).crear(alta.capture(), eq(administrador));

        assertThat(alta.getValue().getPlaca()).isEqualTo("ABC123");
        assertThat(alta.getValue().getCasaId()).isEqualTo(10L);
        assertThat(solicitud.getEstado()).isEqualTo(Codigos.SOLICITUD_APROBADA);
    }

    @Test
    @DisplayName("una solicitud ya resuelta no se vuelve a decidir")
    void nadaDeDecidirDosVeces() {
        solicitud.setEstado(Codigos.SOLICITUD_APROBADA);

        assertThatThrownBy(() -> servicio.aprobar(7L, administrador))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLICITUD_NO_PENDIENTE);

        verify(vehiculoService, never()).crear(any(), any());
    }

    @Test
    @DisplayName("una solicitud de otra sede no existe para este administrador")
    void nadaDeOtraSede() {
        UsuarioAutenticado deOtraSede = new UsuarioAutenticado(8L, 80L, 2L, "2000",
                "Admin de otra sede", Codigos.ROL_ADMIN);

        assertThatThrownBy(() -> servicio.aprobar(7L, deOtraSede))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);

        verify(vehiculoService, never()).crear(any(), any());
    }

    @Test
    @DisplayName("rechazar guarda el motivo y no crea nada")
    void rechazarGuardaElMotivo() {
        servicio.rechazar(7L, "  Esa placa no corresponde a tu casa  ", administrador);

        assertThat(solicitud.getEstado()).isEqualTo(Codigos.SOLICITUD_RECHAZADA);
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("Esa placa no corresponde a tu casa");
        verify(vehiculoService, never()).crear(any(), any());
    }

    @Test
    @DisplayName("un motivo en blanco queda nulo, no como cadena vacia")
    void motivoEnBlancoEsNulo() {
        servicio.rechazar(7L, "   ", administrador);

        assertThat(solicitud.getMotivoRechazo()).isNull();
    }
}
