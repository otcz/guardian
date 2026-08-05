package guardian.repository;

import guardian.entity.vehiculo.GdSolicitudVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdSolicitudVehiculoRepository extends JpaRepository<GdSolicitudVehiculo, Long> {

    /**
     * Lo que la casa tiene en curso o le acaban de responder. Las descartadas
     * salen por {@code activo}: el residente ya leyo el motivo y las quito.
     */
    @Query("SELECT s FROM GdSolicitudVehiculo s "
            + "WHERE s.casa.id = :casaId AND s.activo = :activo "
            + "AND s.estado IN :estados "
            + "ORDER BY s.id DESC")
    List<GdSolicitudVehiculo> listarPorCasa(@Param("casaId") Long casaId,
                                            @Param("activo") String activo,
                                            @Param("estados") List<String> estados);

    /**
     * Una placa no se puede pedir dos veces mientras la primera espera. Se
     * busca en TODO el sistema y no solo en la casa: dos hogares pidiendo el
     * mismo carro es justo el conflicto que hay que cortar antes, no al
     * aprobar.
     */
    Optional<GdSolicitudVehiculo> findFirstByPlacaAndEstado(String placa, String estado);

    /**
     * La bandeja del administrador. Con FETCH de casa y solicitante porque el
     * mapeo lee el identificador de la casa y el nombre de quien pidio.
     */
    @Query("SELECT s FROM GdSolicitudVehiculo s "
            + "JOIN FETCH s.casa c "
            + "JOIN FETCH s.solicitante p "
            + "WHERE c.conjunto.id = :conjuntoId AND s.estado = :estado "
            + "ORDER BY s.fechaCreacion ASC")
    List<GdSolicitudVehiculo> listarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                                       @Param("estado") String estado);

    @Query("SELECT COUNT(s) FROM GdSolicitudVehiculo s "
            + "WHERE s.casa.conjunto.id = :conjuntoId AND s.estado = :estado")
    long contarPorConjuntoYEstado(@Param("conjuntoId") Long conjuntoId,
                                  @Param("estado") String estado);
}
