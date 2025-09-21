package otcz.guardian.repository.usuario;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    // Buscar usuario por correo (login)
    Optional<UsuarioEntity> findByCorreo(String correo);

    // Buscar usuario por documento
    Optional<UsuarioEntity> findByDocumentoNumero(String documentoNumero);

    // Listar todos los usuarios por rol
    List<UsuarioEntity> findByRol(Rol rol);

    // Listar usuarios activos
    List<UsuarioEntity> findByEstado(EstadoUsuario estado);

    // Buscar por rol y estado
    List<UsuarioEntity> findByRolAndEstado(Rol rol, EstadoUsuario estado);

    // Buscar usuarios por casa
    List<UsuarioEntity> findByCasa(String casa);
}
