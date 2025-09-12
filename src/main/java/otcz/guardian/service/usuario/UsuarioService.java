package otcz.guardian.service.usuario;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UsuarioService extends UserDetailsService {

    UsuarioEntity crearUsuario(UsuarioEntity usuarioEntity);

    UsuarioEntity actualizarUsuario(UsuarioEntity usuarioEntity);

    void eliminarUsuario(Long id);

    Optional<UsuarioEntity> obtenerUsuarioPorId(Long id);

    Optional<UsuarioEntity> obtenerUsuarioPorCorreo(String correo);

    Optional<UsuarioEntity> obtenerUsuarioPorDocumento(String documentoNumero);

    List<UsuarioEntity> listarUsuariosPorRol(Rol rol);

    List<UsuarioEntity> listarUsuariosPorEstado(EstadoUsuario estado);
}
