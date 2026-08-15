package guardian.repository;

import guardian.entity.persona.GdPersona;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdPersonaRepository
        extends JpaRepository<GdPersona, Long>, JpaSpecificationExecutor<GdPersona> {

    Optional<GdPersona> findByConjuntoIdAndDocumento(Long conjuntoId, String documento);

    // Unicidad GLOBAL: el documento identifica a una persona en todo el
    // sistema, no dentro de su sede.
    Optional<GdPersona> findByDocumento(String documento);

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

    /**
     * Candidatos por nombre para la porteria, ordenados alfabeticamente.
     *
     * <p>Consulta propia y no la Specification del panel: aquella pagina,
     * filtra por estado y trae el objeto entero. Aca hace falta lo contrario
     * —pocas columnas, tope duro, y SOLO quien puede pasar— porque quien la
     * dispara es un guardia con gente esperando.</p>
     *
     * <p>Deja fuera a la persona bloqueada y a la inactiva a proposito:
     * ofrecerle al guardia un nombre que el registro va a rechazar le hace
     * perder el toque y la fila crece. Quien no aparece aca es que no pasa.</p>
     */
    @Query("SELECT p FROM GdPersona p "
            + "WHERE p.conjunto.id = :conjuntoId "
            + "AND p.activo = 'S' AND p.bloqueado = 'N' "
            + "AND LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :texto, '%')) "
            + "ORDER BY p.nombres ASC, p.apellidos ASC")
    List<GdPersona> buscarPorNombre(@Param("conjuntoId") Long conjuntoId,
                                    @Param("texto") String texto,
                                    Pageable limite);
}
