package otcz.guardian.service.usuario.invitado;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.usuario.invitado.Invitado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitadoService {

    Invitado crearInvitado(Invitado invitado);

    Invitado actualizarInvitado(Invitado invitado);

    void eliminarInvitado(Long id);

    Optional<Invitado> obtenerPorCodigoQR(String codigoQR);

    List<Invitado> listarInvitadosPorUsuario(UsuarioEntity usuarioEntity);

    List<Invitado> listarInvitadosActivosPorUsuario(UsuarioEntity usuarioEntity);

    List<Invitado> listarInvitadosValidos(LocalDateTime fecha);
}
