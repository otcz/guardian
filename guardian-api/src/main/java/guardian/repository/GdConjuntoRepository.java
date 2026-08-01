package guardian.repository;

import guardian.entity.conjunto.GdConjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdConjuntoRepository extends JpaRepository<GdConjunto, Long> {

    Optional<GdConjunto> findFirstByOrderByIdAsc();

    /**
     * Las sedes REALES. La fila de plataforma queda fuera a proposito: no es
     * una sede, es donde cuelga el super administrador.
     */
    List<GdConjunto> findByEsPlataformaOrderByNombreAsc(String esPlataforma);

    Optional<GdConjunto> findFirstByEsPlataforma(String esPlataforma);

    Optional<GdConjunto> findFirstByNombreIgnoreCaseAndEsPlataforma(
            String nombre, String esPlataforma);
}
