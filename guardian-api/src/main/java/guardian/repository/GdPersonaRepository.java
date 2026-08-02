package guardian.repository;

import guardian.entity.persona.GdPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GdPersonaRepository
        extends JpaRepository<GdPersona, Long>, JpaSpecificationExecutor<GdPersona> {

    Optional<GdPersona> findByConjuntoIdAndDocumento(Long conjuntoId, String documento);

    // Unicidad GLOBAL: el documento y el telefono identifican a una persona en
    // todo el sistema, no dentro de su sede.
    Optional<GdPersona> findByDocumento(String documento);

    Optional<GdPersona> findByTelefono(String telefono);

    /** Siempre en minusculas: ver CorreoUtil. Es la llave de recuperacion. */
    Optional<GdPersona> findByEmail(String email);

    boolean existsByConjuntoIdAndDocumento(Long conjuntoId, String documento);

    long countByConjuntoIdAndActivo(Long conjuntoId, String activo);

    /**
     * Los mismos conteos SIN una persona: el administrador no se cuenta a si
     * mismo en su tablero, igual que no se ve en sus listados. Sin esto el
     * Resumen decia "1 persona activa" sobre una lista vacia.
     */
    long countByConjuntoIdAndActivoAndIdNot(Long conjuntoId, String activo, Long id);

    long countByConjuntoIdAndIdNot(Long conjuntoId, Long id);

    long countByConjuntoId(Long conjuntoId);

    // La busqueda por texto libre vive en
    // guardian.repository.spec.PersonaSpecs — ver ahi por que no es un @Query.
}
