package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdInvitacionRepository;
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
 * Aprobar una invitacion no crea nada nuevo: solo deja que el QR ya emitido
 * empiece a servir en la porteria (ver AccesoInvitadoServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitacionAprobacionServiceImplTest {

    @Mock private GdInvitacionRepository invitacionRepository;

    @InjectMocks
    private InvitacionAprobacionServiceImpl servicio;

    private UsuarioAutenticado admin;
    private GdInvitacion pendiente;

    @BeforeEach
    void preparar() {
        admin = new UsuarioAutenticado(1L, 1L, 1L, "ADMIN", "Admin", Codigos.ROL_ADMIN);

        GdCasa casa = new GdCasa();
        casa.setId(3L);
        casa.setIdentificador("M1-C5");

        GdPersona anfitrion = new GdPersona();
        anfitrion.setNombres("Ana");
        anfitrion.setApellidos("Diaz");

        pendiente = new GdInvitacion();
        pendiente.setId(20L);
        pendiente.setConjuntoId(1L);
        pendiente.setCasa(casa);
        pendiente.setAnfitrion(anfitrion);
        pendiente.setNombreInvitado("Pedro Perez");
        pendiente.setDocumentoInvitado("999");
        pendiente.setEstadoAprobacion(Codigos.SOLICITUD_PENDIENTE);

        lenient().when(invitacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("aprobar deja el estado en APROBADA y no toca nada mas")
    void aprobarSoloCambiaElEstado() {
        when(invitacionRepository.findById(20L)).thenReturn(Optional.of(pendiente));

        servicio.aprobar(20L, admin);

        assertThat(pendiente.getEstadoAprobacion()).isEqualTo(Codigos.SOLICITUD_APROBADA);
        assertThat(pendiente.estaAprobada()).isTrue();
    }

    @Test
    @DisplayName("rechazar guarda el motivo")
    void rechazarGuardaElMotivo() {
        when(invitacionRepository.findById(20L)).thenReturn(Optional.of(pendiente));

        servicio.rechazar(20L, "El anfitrion no tiene casa activa", admin);

        assertThat(pendiente.getEstadoAprobacion()).isEqualTo(Codigos.SOLICITUD_RECHAZADA);
        assertThat(pendiente.getMotivoRechazo()).isEqualTo("El anfitrion no tiene casa activa");
    }

    @Test
    @DisplayName("una invitacion ya resuelta no se puede volver a aprobar")
    void noSeResuelveDosVeces() {
        pendiente.setEstadoAprobacion(Codigos.SOLICITUD_APROBADA);
        when(invitacionRepository.findById(20L)).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> servicio.aprobar(20L, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
    }

    @Test
    @DisplayName("una invitacion de otra sede no se puede resolver")
    void noSePuedeResolverDeOtraSede() {
        pendiente.setConjuntoId(99L);
        when(invitacionRepository.findById(20L)).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> servicio.rechazar(20L, "motivo", admin))
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);
    }
}
