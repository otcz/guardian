package guardian.service.acceso;

import guardian.dto.invitacion.InvitacionPublicaResponse;
import guardian.dto.invitacion.InvitacionRequest;
import guardian.dto.invitacion.InvitacionResponse;
import guardian.entity.acceso.GdInvitacion;
import guardian.security.UsuarioAutenticado;

import java.util.List;
import java.util.Optional;

public interface InvitacionService {

    /** Crea la invitacion sobre la casa del anfitrion en sesion. */
    InvitacionResponse crear(InvitacionRequest request, UsuarioAutenticado anfitrion);

    /** Invitaciones de MI casa — las ve cualquier residente de ella. */
    List<InvitacionResponse> listarDeMiCasa(UsuarioAutenticado usuario);

    /** Revocacion por un residente de la casa. */
    InvitacionResponse revocar(Long id, UsuarioAutenticado usuario);

    /**
     * Borra la invitacion de la lista del residente.
     *
     * <p>Distinto de revocar. Revocar mata el codigo y deja la fila a la vista
     * con su estado; eliminar la saca de la lista. Despues de un mes invitando
     * gente, la pantalla del residente son cuarenta filas vencidas y encontrar
     * la de hoy cuesta mas que crear una nueva.</p>
     *
     * <p>Lo que no se borra es la bitacora: los ingresos que esa invitacion
     * permitio son del conjunto, no del residente. Los eventos se desvinculan y
     * conservan el nombre y el documento del invitado, que ya venian
     * denormalizados justamente para esto.</p>
     */
    void eliminar(Long id, UsuarioAutenticado usuario);

    /** Todas las del conjunto — panel del administrador. */
    List<InvitacionResponse> listarDelConjunto(Long conjuntoId);

    InvitacionResponse revocarComoAdmin(Long id, UsuarioAutenticado admin);

    /** Mismo borrado, sobre cualquier invitacion de la sede. */
    void eliminarComoAdmin(Long id, UsuarioAutenticado admin);

    /** Datos para la pagina publica que abre el invitado desde el link. */
    InvitacionPublicaResponse publica(String codigoPublico);

    /**
     * Resuelve un payload GRDI escaneado en porteria. Solo firma y existencia;
     * vigencia, usos y revocacion las evalua AccesoService, que ademas registra
     * el intento fallido.
     */
    Optional<GdInvitacion> resolver(String payload);

    String construirPayload(GdInvitacion invitacion);
}
