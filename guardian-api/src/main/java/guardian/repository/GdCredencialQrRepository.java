package guardian.repository;

import guardian.entity.acceso.GdCredencialQr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdCredencialQrRepository extends JpaRepository<GdCredencialQr, Long> {

    /**
     * Resolucion del QR escaneado. Trae persona y conjunto en el mismo query:
     * la garita necesita la foto y la casa de inmediato y no puede pagar dos
     * viajes mas a la base con gente esperando en la porteria.
     */
    @Query("SELECT c FROM GdCredencialQr c "
            + "JOIN FETCH c.persona p "
            + "JOIN FETCH p.conjunto "
            + "WHERE c.codigoPublico = :codigoPublico")
    Optional<GdCredencialQr> buscarPorCodigoPublico(@Param("codigoPublico") String codigoPublico);

    List<GdCredencialQr> findByPersonaIdOrderByIdDesc(Long personaId);

    Optional<GdCredencialQr> findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
            Long personaId, String tipo, String activo);

    /** Solo para la eliminacion fisica de la persona (exclusiva del admin). */
    void deleteByPersonaId(Long personaId);
}
