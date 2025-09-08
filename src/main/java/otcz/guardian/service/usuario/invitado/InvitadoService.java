package otcz.guardian.service.usuario.invitado;

import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.usuario.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitadoService {

    Invitado crearInvitado(Invitado invitado);

    Invitado actualizarInvitado(Invitado invitado);

    void eliminarInvitado(Long id);

    Optional<Invitado> obtenerPorCodigoQR(String codigoQR);

    List<Invitado> listarInvitadosPorUsuario(Usuario usuario);

    List<Invitado> listarInvitadosActivosPorUsuario(Usuario usuario);

    List<Invitado> listarInvitadosValidos(LocalDateTime fecha);
}
