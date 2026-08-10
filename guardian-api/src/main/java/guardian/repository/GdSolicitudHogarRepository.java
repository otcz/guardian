package guardian.repository;

import guardian.entity.persona.GdSolicitudHogar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GdSolicitudHogarRepository extends JpaRepository<GdSolicitudHogar, Long> {

    /** Si el codigo ya tiene una solicitud sin resolver, no admite una segunda. */
    boolean existsByCodigoIdAndEstado(Long codigoId, String estado);

    /**
     * La bandeja del administrador. Con FETCH del codigo y su casa porque el
     * mapeo lee el identificador de la casa de cada fila.
     */
    @Query("SELECT s FROM GdSolicitudHogar s "
            + "JOIN FETCH s.codigo c "
            + "JOIN FETCH c.casa "
            + "WHERE c.casa.conjunto.id = :conjuntoId AND s.estado = :estado "
            + "ORDER BY s.fechaCreacion ASC")
    List<GdSolicitudHogar> listarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                                    @Param("estado") String estado);

    @Query("SELECT COUNT(s) FROM GdSolicitudHogar s "
            + "WHERE s.codigo.casa.conjunto.id = :conjuntoId AND s.estado = :estado")
    long contarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                  @Param("estado") String estado);
}
