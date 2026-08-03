package guardian.repository;

import guardian.entity.conjunto.GdPuntoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdPuntoAccesoRepository extends JpaRepository<GdPuntoAcceso, Long> {

    List<GdPuntoAcceso> findByConjuntoIdAndActivoOrderByNombreAsc(Long conjuntoId, String activo);

    /** Todas, incluidas las inactivas: el administrador tiene que poder reactivarlas. */
    List<GdPuntoAcceso> findByConjuntoIdOrderByNombreAsc(Long conjuntoId);

    Optional<GdPuntoAcceso> findFirstByConjuntoIdAndNombreIgnoreCase(Long conjuntoId, String nombre);

    /**
     * Por id PERO dentro de la sede. El id llega de la tablet, o sea de fuera:
     * un findById suelto deja estampar la porteria de otro conjunto sobre un
     * evento de este.
     */
    Optional<GdPuntoAcceso> findByIdAndConjuntoId(Long id, Long conjuntoId);

    long countByConjuntoId(Long conjuntoId);

    long countByConjuntoIdAndActivo(Long conjuntoId, String activo);
}
