package guardian.service.admin;

import guardian.dto.admin.SolicitudVehiculoAdminResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * La bandeja de vehiculos por autorizar. Aprobar aca es lo que hace que la
 * talanquera suba para esa placa, asi que la decision es del administrador.
 */
public interface SolicitudVehiculoAdminService {

    List<SolicitudVehiculoAdminResponse> pendientes(Long conjuntoId);

    long cuantasPendientes(Long conjuntoId);

    /** Crea el vehiculo de verdad y deja la solicitud como aprobada. */
    SolicitudVehiculoAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor);

    SolicitudVehiculoAdminResponse rechazar(Long id, String motivo, UsuarioAutenticado ejecutor);
}
