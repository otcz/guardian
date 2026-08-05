package guardian.service.residente;

import guardian.dto.residente.CasaDisponibleResponse;
import guardian.dto.residente.SolicitudCasaRequest;
import guardian.dto.residente.SolicitudCasaResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;
import java.util.Optional;

/**
 * Un residente sin casa pidiendo entrar a una.
 *
 * <p>La solicitud NO asigna nada: deja constancia de quien pide que, y la
 * administracion decide. Elegir casa de un clic seria autoservicio de acceso —
 * quien entra a un hogar se queda con sus vehiculos, sus invitaciones y su
 * familia.</p>
 */
public interface SolicitudCasaService {

    /** Las casas de SU sede. Sin nombres de residentes: todavia no vive ahi. */
    List<CasaDisponibleResponse> casasDisponibles(UsuarioAutenticado solicitante);

    /** Lo ultimo que le paso a su solicitud, pendiente o ya resuelta. */
    Optional<SolicitudCasaResponse> miSolicitud(UsuarioAutenticado solicitante);

    SolicitudCasaResponse solicitar(SolicitudCasaRequest request, UsuarioAutenticado solicitante);
}
