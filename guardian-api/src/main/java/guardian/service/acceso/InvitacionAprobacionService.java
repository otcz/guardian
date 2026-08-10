package guardian.service.acceso;

import guardian.dto.admin.InvitacionPendienteResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * La bandeja de invitaciones esperando aprobacion y su resolucion.
 *
 * <p>Aparte de {@link InvitacionService} a proposito: esa interfaz ya tiene
 * diez metodos publicos entre creacion, consulta y administracion basica, y
 * sumarle aprobar/rechazar la haria crecer mas de lo que CLAUDE.md permite
 * para un service (regla #3).</p>
 */
public interface InvitacionAprobacionService {

    List<InvitacionPendienteResponse> pendientes(Long conjuntoId);

    long cuantasPendientes(Long conjuntoId);

    /** Aprobar no crea nada nuevo: solo deja que el QR ya emitido sirva. */
    InvitacionPendienteResponse aprobar(Long id, UsuarioAutenticado ejecutor);

    InvitacionPendienteResponse rechazar(Long id, String motivo, UsuarioAutenticado ejecutor);
}
