package otcz.guardian.service.usuario;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetailsService;
import otcz.guardian.DTO.usuario.UsuarioResponseDTO;
import org.springframework.http.ResponseEntity;

public interface UsuarioService extends UserDetailsService {

    void eliminarUsuario(Long id);

    Optional<UsuarioEntity> obtenerUsuarioPorId(Long id);

    Optional<UsuarioEntity> obtenerUsuarioPorCorreo(String correo);

    Optional<UsuarioEntity> obtenerUsuarioPorDocumento(String documentoNumero);

    List<UsuarioEntity> listarUsuariosPorRol(Rol rol);

    List<UsuarioEntity> listarUsuariosPorEstado(EstadoUsuario estado);

    List<UsuarioEntity> listarUsuariosPorCasa(String casa);

    List<UsuarioEntity> listarTodosLosUsuarios();

    UsuarioResponseDTO mapToResponseDTO(UsuarioEntity entity);

    ResponseEntity<?> crearUsuarioDesdeDTO(otcz.guardian.DTO.usuario.UsuarioRequestDTO usuarioDTO);

    ResponseEntity<?> actualizarUsuarioDesdeDTO(Long id, otcz.guardian.DTO.usuario.UsuarioRequestDTO usuarioDTO);
}
