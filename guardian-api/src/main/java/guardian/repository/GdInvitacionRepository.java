package guardian.repository;

import guardian.entity.acceso.GdInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdInvitacionRepository extends JpaRepository<GdInvitacion, Long> {

    /**
     * Resolucion del QR escaneado o del link publico. Trae casa y anfitrion en
     * el mismo query: la ficha de porteria los necesita de inmediato.
     */
    @Query("SELECT i FROM GdInvitacion i "
            + "JOIN FETCH i.casa "
            + "JOIN FETCH i.anfitrion "
            + "WHERE i.codigoPublico = :codigoPublico")
    Optional<GdInvitacion> buscarPorCodigoPublico(@Param("codigoPublico") String codigoPublico);

    /**
     * Las invitaciones emitidas a ese documento en el conjunto.
     *
     * <p>Es el camino de la porteria cuando el invitado llega SIN el codigo:
     * dice su nombre y entrega la cedula, que es justo el documento que el
     * anfitrion declaro al invitarlo. Trae casa y anfitrion en el mismo query
     * porque la ficha los necesita de inmediato.</p>
     *
     * <p>Ordenadas por fin de vigencia descendente: si hay varias, la primera
     * es la mas reciente, que es de la que el guardia quiere saber.</p>
     */
    @Query("SELECT i FROM GdInvitacion i "
            + "JOIN FETCH i.casa "
            + "JOIN FETCH i.anfitrion "
            + "WHERE i.conjuntoId = :conjuntoId "
            + "AND TRIM(i.documentoInvitado) = :documento "
            + "ORDER BY i.vigenciaHasta DESC")
    List<GdInvitacion> buscarPorDocumentoInvitado(@Param("conjuntoId") Long conjuntoId,
                                                  @Param("documento") String documento);

    List<GdInvitacion> findByCasaIdOrderByIdDesc(Long casaId);

    List<GdInvitacion> findByConjuntoIdOrderByIdDesc(Long conjuntoId);

    /**
     * La bandeja del administrador. Con FETCH de casa y anfitrion porque el
     * mapeo lee nombre e identificador de cada fila.
     */
    @Query("SELECT i FROM GdInvitacion i "
            + "JOIN FETCH i.casa "
            + "JOIN FETCH i.anfitrion "
            + "WHERE i.conjuntoId = :conjuntoId AND i.estadoAprobacion = :estado "
            + "ORDER BY i.fechaCreacion ASC")
    List<GdInvitacion> listarPorConjuntoYEstadoAprobacion(@Param("conjuntoId") Long conjuntoId,
                                                          @Param("estado") String estado);

    long countByConjuntoIdAndEstadoAprobacion(Long conjuntoId, String estadoAprobacion);

    /** Solo para la eliminacion fisica del anfitrion (exclusiva del admin). */
    void deleteByAnfitrionId(Long anfitrionId);
}
