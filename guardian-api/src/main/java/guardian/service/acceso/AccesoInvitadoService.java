package guardian.service.acceso;

import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.entity.acceso.GdInvitacion;
import guardian.security.UsuarioAutenticado;

/**
 * Flujo de porteria para INVITADOS. La invitacion ya llega resuelta y con
 * firma valida: resolverla es de {@link InvitacionService}; aqui viven las
 * puertas, el registro y la ficha.
 */
public interface AccesoInvitadoService {

    FichaVerificacionResponse verificar(GdInvitacion invitacion,
                                        VerificarQrRequest request,
                                        UsuarioAutenticado guardia);

    AccesoEventoResponse registrar(GdInvitacion invitacion,
                                   RegistrarAccesoRequest request,
                                   UsuarioAutenticado guardia);
}
