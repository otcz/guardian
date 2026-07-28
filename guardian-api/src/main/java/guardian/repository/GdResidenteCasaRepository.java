package guardian.repository;

import guardian.entity.persona.GdResidenteCasa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdResidenteCasaRepository extends JpaRepository<GdResidenteCasa, Long> {

    List<GdResidenteCasa> findByPersonaIdAndActivo(Long personaId, String activo);

    List<GdResidenteCasa> findByCasaIdAndActivo(Long casaId, String activo);

    Optional<GdResidenteCasa> findByPersonaIdAndCasaId(Long personaId, Long casaId);

    Optional<GdResidenteCasa> findFirstByCasaIdAndParentescoAndActivo(
            Long casaId, String parentesco, String activo);

    /**
     * Vinculo vigente de la persona. Cuando alguien figura en dos unidades se
     * toma el mas antiguo, que en la practica es su residencia principal: la
     * segunda suele ser un apartamento heredado o arrendado.
     */
    Optional<GdResidenteCasa> findFirstByPersonaIdAndActivoOrderByIdAsc(
            Long personaId, String activo);
}
