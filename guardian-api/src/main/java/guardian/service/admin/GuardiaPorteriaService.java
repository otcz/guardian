package guardian.service.admin;

import guardian.dto.admin.AsignarGuardiasRequest;
import guardian.dto.admin.GuardiaPorteriaResponse;
import guardian.dto.admin.PuntoAccesoResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * Que guardias trabajan en cada porteria.
 *
 * <p>Aparte de {@link PuntoAccesoService} porque es otra responsabilidad: aquel
 * administra las puertas, este el reparto de la gente entre ellas.</p>
 */
public interface GuardiaPorteriaService {

    /**
     * Los guardias de la sede mas los que ya esten asignados a esta porteria,
     * cada uno con su bandera. Una sola lista: la pantalla es una lista de
     * chequeo y partirla obligaria a fusionarla en el cliente.
     */
    List<GuardiaPorteriaResponse> listar(Long porteriaId, UsuarioAutenticado ejecutor);

    /**
     * Deja en la porteria EXACTAMENTE a los que vengan en la peticion.
     *
     * <p>Quitar a alguien desactiva su fila, nunca la borra: saber quien estuvo
     * asignado el dia de un incidente es justo lo que se pregunta despues.</p>
     */
    PuntoAccesoResponse asignar(Long porteriaId, AsignarGuardiasRequest request,
                                UsuarioAutenticado ejecutor);
}
