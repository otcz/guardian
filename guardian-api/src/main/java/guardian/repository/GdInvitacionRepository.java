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

    List<GdInvitacion> findByCasaIdOrderByIdDesc(Long casaId);

    List<GdInvitacion> findByConjuntoIdOrderByIdDesc(Long conjuntoId);

    /** Solo para la eliminacion fisica del anfitrion (exclusiva del admin). */
    void deleteByAnfitrionId(Long anfitrionId);
}
