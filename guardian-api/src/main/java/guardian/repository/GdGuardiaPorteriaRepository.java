package guardian.repository;

import guardian.entity.conjunto.GdGuardiaPorteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdGuardiaPorteriaRepository extends JpaRepository<GdGuardiaPorteria, Long> {

    /**
     * Todas las filas de la porteria, activas o no.
     *
     * <p>El FETCH de la persona no es opcional: el mapeo lee nombre y documento
     * de cada fila y sin el son N consultas mas por cada porteria del panel.</p>
     */
    @Query("SELECT g FROM GdGuardiaPorteria g "
            + "JOIN FETCH g.persona p "
            + "WHERE g.puntoAcceso.id = :puntoAccesoId "
            + "ORDER BY p.apellidos, p.nombres")
    List<GdGuardiaPorteria> listarDeLaPorteria(@Param("puntoAccesoId") Long puntoAccesoId);

    Optional<GdGuardiaPorteria> findByPersonaIdAndPuntoAccesoId(Long personaId, Long puntoAccesoId);

    long countByPuntoAccesoIdAndActivo(Long puntoAccesoId, String activo);

    /**
     * Las porterias donde este guardia esta asignado hoy. Con el filtro de sede
     * en el propio query: la asignacion vive en otra tabla y sin esto una fila
     * vieja de otro conjunto se colaria en la sugerencia de la tablet.
     */
    @Query("SELECT g.puntoAcceso.id FROM GdGuardiaPorteria g "
            + "WHERE g.persona.id = :personaId "
            + "AND g.activo = :activo "
            + "AND g.puntoAcceso.conjunto.id = :conjuntoId "
            + "AND g.puntoAcceso.activo = :activo")
    List<Long> porteriasAsignadasA(@Param("personaId") Long personaId,
                                   @Param("conjuntoId") Long conjuntoId,
                                   @Param("activo") String activo);
}
