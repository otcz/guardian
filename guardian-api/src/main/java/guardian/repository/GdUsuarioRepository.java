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

    /** Solo para la eliminacion fisica de la persona (exclusiva del admin). */
    void deleteByPersonaId(Long personaId);

    @Query("SELECT COUNT(u) FROM GdUsuario u WHERE u.persona.conjunto.id = :conjuntoId")
    long contarPorConjunto(@Param("conjuntoId") Long conjuntoId);

    @Query("SELECT COUNT(u) FROM GdUsuario u "
            + "WHERE u.persona.conjunto.id = :conjuntoId AND u.activo = :activo")
    long contarPorConjuntoYActivo(@Param("conjuntoId") Long conjuntoId,
                                  @Param("activo") String activo);

    List<GdUsuario> findByRol(String rol);

    /**
     * Login. Trae la persona y el conjunto en el mismo query porque el token se
     * arma con datos de las tres entidades y con open-in-view=false una relacion
     * LAZY reventaria al salir de la transaccion.
     *
     * <p>La comparacion ignora mayusculas: los documentos numericos no cambian,
     * y un usuario alfabetico como ADMIN debe entrar aunque lo escriban
     * "admin".</p>
     */
    @Query("SELECT u FROM GdUsuario u "
            + "JOIN FETCH u.persona p "
            + "JOIN FETCH p.conjunto "
            + "WHERE UPPER(p.documento) = UPPER(:documento)")
    Optional<GdUsuario> buscarPorDocumento(@Param("documento") String documento);
}
