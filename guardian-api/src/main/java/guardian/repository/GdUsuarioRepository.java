package guardian.repository;

import guardian.entity.persona.GdUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdUsuarioRepository extends JpaRepository<GdUsuario, Long> {

    Optional<GdUsuario> findByPersonaId(Long personaId);

    boolean existsByPersonaId(Long personaId);

    List<GdUsuario> findByRol(String rol);

    /**
     * Login. Trae la persona y el conjunto en el mismo query porque el token se
     * arma con datos de las tres entidades y con open-in-view=false una relacion
     * LAZY reventaria al salir de la transaccion.
     */
    @Query("SELECT u FROM GdUsuario u "
            + "JOIN FETCH u.persona p "
            + "JOIN FETCH p.conjunto "
            + "WHERE p.documento = :documento")
    Optional<GdUsuario> buscarPorDocumento(@Param("documento") String documento);
}
