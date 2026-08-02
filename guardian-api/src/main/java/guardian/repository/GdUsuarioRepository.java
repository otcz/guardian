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

    /** Los mismos conteos sin la cuenta del que mira el tablero. */
    @Query("SELECT COUNT(u) FROM GdUsuario u "
            + "WHERE u.persona.conjunto.id = :conjuntoId AND u.id <> :excepto")
    long contarPorConjuntoExcepto(@Param("conjuntoId") Long conjuntoId,
                                  @Param("excepto") Long excepto);

    @Query("SELECT COUNT(u) FROM GdUsuario u WHERE u.persona.conjunto.id = :conjuntoId "
            + "AND u.activo = :activo AND u.id <> :excepto")
    long contarPorConjuntoYActivoExcepto(@Param("conjuntoId") Long conjuntoId,
                                         @Param("activo") String activo,
                                         @Param("excepto") Long excepto);

    @Query("SELECT COUNT(u) FROM GdUsuario u "
            + "WHERE u.persona.conjunto.id = :conjuntoId AND u.activo = :activo")
    long contarPorConjuntoYActivo(@Param("conjuntoId") Long conjuntoId,
                                  @Param("activo") String activo);

    List<GdUsuario> findByRol(String rol);

    /** Si una sede ya tiene administrador. Sirve para no crear un segundo. */
    @Query("SELECT COUNT(u) > 0 FROM GdUsuario u "
            + "WHERE u.persona.conjunto.id = :conjuntoId AND u.rol = :rol")
    boolean existeRolEnConjunto(@Param("conjuntoId") Long conjuntoId, @Param("rol") String rol);

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

    /**
     * Estado fresco para el filtro de seguridad.
     *
     * <p>El JOIN FETCH del CONJUNTO no es opcional: este codigo corre en el
     * filtro, FUERA de transaccion y con open-in-view=false, asi que leer
     * {@code persona.getConjunto()} sin traerlo lanzaria
     * LazyInitializationException en CADA peticion autenticada de CADA
     * usuario — con un stacktrace que no menciona sedes por ningun lado.</p>
     */
    @Query("SELECT u FROM GdUsuario u "
            + "JOIN FETCH u.persona p "
            + "JOIN FETCH p.conjunto "
            + "WHERE u.id = :id")
    Optional<GdUsuario> buscarConPersona(@Param("id") Long id);

    /**
     * Listado del back-office. El filtro por conjunto va en el query — no en
     * memoria — y los FETCH evitan el N+1 del mapeo (persona y conjunto se
     * leen para cada fila del panel).
     */
    @Query("SELECT u FROM GdUsuario u "
            + "JOIN FETCH u.persona p "
            + "JOIN FETCH p.conjunto c "
            + "WHERE c.id = :conjuntoId "
            + "ORDER BY p.apellidos, p.nombres")
    List<GdUsuario> listarPorConjunto(@Param("conjuntoId") Long conjuntoId);
}
