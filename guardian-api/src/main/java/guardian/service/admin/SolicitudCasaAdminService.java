package guardian.service.admin;

import guardian.dto.admin.SolicitudCasaAdminResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/** La bandeja de solicitudes de casa y su resolucion. */
public interface SolicitudCasaAdminService {

    List<SolicitudCasaAdminResponse> pendientes(Long conjuntoId);

    long cuantasPendientes(Long conjuntoId);

    /**
     * Aprueba y VINCULA a la persona con la casa.
     *
     * <p>El vinculo se crea aca y no lo crea la solicitud: entre pedir y
     * aprobar puede haber pasado cualquier cosa —que la casa consiguiera
     * titular, que la persona entrara a otra— asi que las reglas se vuelven a
     * comprobar en el momento de decidir, no en el de pedir.</p>
     */
    SolicitudCasaAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor);

    SolicitudCasaAdminResponse rechazar(Long id, String motivo, UsuarioAutenticado ejecutor);
}
