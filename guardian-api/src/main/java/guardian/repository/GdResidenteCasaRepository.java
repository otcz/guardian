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
     * Vinculo vigente de la persona. Se usa para lo que el residente
     * administra: su hogar, sus vehiculos, sus invitaciones.
     */
    Optional<GdResidenteCasa> findFirstByPersonaIdAndActivoOrderByIdAsc(
            Long personaId, String activo);

    /**
     * Vinculo SIN filtrar por activo. Es el que usa la porteria para evaluar
     * el ingreso: un vinculo apagado no puede servir para escaparse del
     * bloqueo de la casa. Con una persona por nucleo, solo hay uno.
     */
    Optional<GdResidenteCasa> findFirstByPersonaIdOrderByIdAsc(Long personaId);

    /** Solo para la eliminacion fisica de la persona (exclusiva del admin). */
    void deleteByPersonaId(Long personaId);
}
