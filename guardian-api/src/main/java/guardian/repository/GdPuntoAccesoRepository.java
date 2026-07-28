package guardian.repository;

import guardian.entity.conjunto.GdPuntoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GdPuntoAccesoRepository extends JpaRepository<GdPuntoAcceso, Long> {

    List<GdPuntoAcceso> findByConjuntoIdAndActivoOrderByNombreAsc(Long conjuntoId, String activo);

    long countByConjuntoId(Long conjuntoId);
}
