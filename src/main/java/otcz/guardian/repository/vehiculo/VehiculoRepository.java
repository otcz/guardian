package otcz.guardian.repository.vehiculo;

import otcz.guardian.entity.vehiculo.VehiculoEntity;
 import otcz.guardian.utils.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<VehiculoEntity, Long> {

    // Buscar vehículo por placa
    Optional<VehiculoEntity> findByPlaca(String placa);

    // Listar vehículos por tipo
    List<VehiculoEntity> findByTipo(TipoVehiculo tipo);
}
