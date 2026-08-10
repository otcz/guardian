package guardian.service.admin;

import guardian.dto.admin.SolicitudHogarAdminResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/** La bandeja de solicitudes de union al hogar y su resolucion. */
public interface SolicitudHogarAdminService {

    List<SolicitudHogarAdminResponse> pendientes(Long conjuntoId);

    long cuantasPendientes(Long conjuntoId);

    /**
     * Aprueba y CREA a la persona, su cuenta y su vinculo con la casa.
     *
     * <p>Nada de eso existe hasta este momento: la solicitud solo guardo los
     * datos. Entre pedir y aprobar el documento pudo registrarse por otro
     * camino, asi que la unicidad se vuelve a comprobar aca.</p>
     */
    SolicitudHogarAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor);

    /** Rechaza sin tocar el codigo: sigue sirviendo para un nuevo intento. */
    SolicitudHogarAdminResponse rechazar(Long id, String motivo, UsuarioAutenticado ejecutor);
}
