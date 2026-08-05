package guardian.repository;

import guardian.entity.persona.GdSolicitudCasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdSolicitudCasaRepository extends JpaRepository<GdSolicitudCasa, Long> {

    /** La que el residente tiene en curso. Solo puede haber una. */
    Optional<GdSolicitudCasa> findFirstByPersonaIdAndEstadoOrderByIdDesc(Long personaId,
                                                                        String estado);

    /** Lo ultimo que le paso, este pendiente o ya resuelto: es lo que ve en pantalla. */
    Optional<GdSolicitudCasa> findFirstByPersonaIdOrderByIdDesc(Long personaId);

    /**
     * La bandeja del administrador. Con FETCH de persona y casa porque el mapeo
     * lee nombre, documento e identificador de cada fila.
     */
    @Query("SELECT s FROM GdSolicitudCasa s "
            + "JOIN FETCH s.persona p "
            + "JOIN FETCH s.casa c "
            + "WHERE c.conjunto.id = :conjuntoId AND s.estado = :estado "
            + "ORDER BY s.fechaCreacion ASC")
    List<GdSolicitudCasa> listarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                                   @Param("estado") String estado);

    @Query("SELECT COUNT(s) FROM GdSolicitudCasa s "
            + "WHERE s.casa.conjunto.id = :conjuntoId AND s.estado = :estado")
    long contarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                  @Param("estado") String estado);
}
