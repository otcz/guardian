package guardian.repository;

import guardian.entity.parametro.GdParametro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdParametroRepository extends JpaRepository<GdParametro, Long> {

    List<GdParametro> findByGrupoAndActivoOrderByOrdenAsc(String grupo, String activo);

    /** Activas e inactivas: solo el panel de administracion necesita las dos. */
    List<GdParametro> findByGrupoOrderByOrdenAsc(String grupo);

    long countByGrupoAndActivo(String grupo, String activo);

    Optional<GdParametro> findByGrupoAndCodigo(String grupo, String codigo);

    boolean existsByGrupoAndCodigo(String grupo, String codigo);

    boolean existsByGrupoAndCodigoAndActivo(String grupo, String codigo, String activo);
}
