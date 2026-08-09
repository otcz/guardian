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

    /**
     * Revocacion por un residente de la casa.
     *
     * <p>Es lo unico que puede hacer: la fila se queda con su estado a la
     * vista. Borrarla es del super administrador — ver
     * {@link #eliminarComoSuperAdmin}.</p>
     */
    InvitacionResponse revocar(Long id, UsuarioAutenticado usuario);

    /** Todas las del conjunto — panel del administrador. */
    List<InvitacionResponse> listarDelConjunto(Long conjuntoId);

    InvitacionResponse revocarComoAdmin(Long id, UsuarioAutenticado admin);

    /**
     * Borrado fisico. Solo el super administrador.
     *
     * <p>Que una invitacion desaparezca es un hueco en la auditoria: alguien
     * pudo entrar al conjunto por un codigo del que despues no queda registro
     * de quien lo emitio. Por eso el residente y el administrador solo revocan
     * —el estado queda a la vista— y borrar es del operador de la plataforma,
     * que no tiene interes en la disputa que ese registro pueda resolver.</p>
     *
     * <p>La bitacora nunca se va con el: los eventos se desvinculan y conservan
     * el nombre y el documento del invitado, que ya venian denormalizados
     * justamente para esto.</p>
     */
    void eliminarComoSuperAdmin(Long id, UsuarioAutenticado ejecutor);

    /** Datos para la pagina publica que abre el invitado desde el link. */
    InvitacionPublicaResponse publica(String codigoPublico);

    /**
     * Resuelve un payload GRDI escaneado en porteria. Solo firma y existencia;
     * vigencia, usos y revocacion las evalua AccesoService, que ademas registra
     * el intento fallido.
     */
    Optional<GdInvitacion> resolver(String payload);

    /**
     * La invitacion que responde por ese documento en la porteria.
     *
     * <p>Para cuando el invitado llega sin el codigo —se le acabo la bateria,
     * no le llego el link, lo borro— y entrega la cedula. El documento no es
     * un dato suelto: es el que el anfitrion declaro al invitarlo, y el mismo
     * que la ficha le pide al guardia comparar contra el documento fisico.</p>
     *
     * <p>Si hay varias a ese documento devuelve la que puede pasar AHORA; si
     * ninguna puede, la mas reciente, para que la porteria diga "vencio" o
     * "todavia no empieza" en vez de "no existe". La evaluacion de vigencia,
     * revocacion y usos sigue siendo de AccesoInvitadoService: aca solo se
     * elige de cual hablar.</p>
     */
    Optional<GdInvitacion> buscarPorDocumento(Long conjuntoId, String documento);

    String construirPayload(GdInvitacion invitacion);
}
