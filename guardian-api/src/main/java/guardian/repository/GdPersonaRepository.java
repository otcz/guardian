package guardian.repository;

import guardian.entity.persona.GdPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GdPersonaRepository
        extends JpaRepository<GdPersona, Long>, JpaSpecificationExecutor<GdPersona> {

    Optional<GdPersona> findByConjuntoIdAndDocumento(Long conjuntoId, String documento);

    boolean existsByConjuntoIdAndDocumento(Long conjuntoId, String documento);

    long countByConjuntoIdAndActivo(Long conjuntoId, String activo);

    long countByConjuntoId(Long conjuntoId);

    // La busqueda por texto libre vive en
    // guardian.repository.spec.PersonaSpecs — ver ahi por que no es un @Query.
}
