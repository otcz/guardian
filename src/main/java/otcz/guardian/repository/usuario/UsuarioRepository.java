package otcz.guardian.repository.usuario;

import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por correo (login)
    Optional<Usuario> findByCorreo(String correo);

    // Buscar usuario por documento
    Optional<Usuario> findByDocumentoNumero(String documentoNumero);

    // Listar todos los usuarios por rol
    List<Usuario> findByRol(Rol rol);

    // Listar usuarios activos
    List<Usuario> findByEstado(EstadoUsuario estado);

    // Buscar por rol y estado
    List<Usuario> findByRolAndEstado(Rol rol, EstadoUsuario estado);
}
