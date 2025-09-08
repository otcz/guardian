package otcz.guardian.service.usuario;

import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    Usuario crearUsuario(Usuario usuario);

    Usuario actualizarUsuario(Usuario usuario);

    void eliminarUsuario(Long id);

    Optional<Usuario> obtenerUsuarioPorId(Long id);

    Optional<Usuario> obtenerUsuarioPorCorreo(String correo);

    Optional<Usuario> obtenerUsuarioPorDocumento(String documentoNumero);

    List<Usuario> listarUsuariosPorRol(Rol rol);

    List<Usuario> listarUsuariosPorEstado(EstadoUsuario estado);
}
