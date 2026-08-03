package guardian.service.admin;

import guardian.dto.admin.PuntoAccesoRequest;
import guardian.dto.admin.PuntoAccesoResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * Las porterias del conjunto: por donde se entra y se sale.
 *
 * <p>No hay borrado. Cada evento de la bitacora apunta a la porteria por la que
 * paso la persona; borrarla dejaria el historico sin poder decir por donde
 * entro nadie, que es justo lo que el registro existe para responder.</p>
 */
public interface PuntoAccesoService {

    List<PuntoAccesoResponse> listar(Long conjuntoId);

    PuntoAccesoResponse crear(PuntoAccesoRequest request, UsuarioAutenticado ejecutor);

    PuntoAccesoResponse actualizar(Long id, PuntoAccesoRequest request, UsuarioAutenticado ejecutor);

    /**
     * Habilita o deshabilita la porteria. Deshabilitada deja de ofrecerse en la
     * garita y de recibir eventos nuevos, pero los ya registrados no se tocan.
     */
    PuntoAccesoResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor);
}
