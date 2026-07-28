package guardian.repository;

import guardian.entity.vehiculo.GdVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdVehiculoRepository extends JpaRepository<GdVehiculo, Long> {

    Optional<GdVehiculo> findByConjuntoIdAndPlaca(Long conjuntoId, String placa);

    boolean existsByConjuntoIdAndPlaca(Long conjuntoId, String placa);

    List<GdVehiculo> findByCasaIdAndActivoOrderByPlacaAsc(Long casaId, String activo);

    List<GdVehiculo> findByConjuntoIdOrderByPlacaAsc(Long conjuntoId);
}
