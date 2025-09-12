package otcz.guardian.repository.vehiculo;

import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Buscar vehículo por placa
    Optional<Vehiculo> findByPlaca(String placa);

    // Listar vehículos de un usuario
    List<Vehiculo> findByUsuarioEntity(UsuarioEntity usuarioEntity);

    // Listar vehículos por tipo
    List<Vehiculo> findByTipo(TipoVehiculo tipo);

    // Listar vehículos activos de un usuario
    List<Vehiculo> findByUsuarioEntityAndActivoTrue(UsuarioEntity usuarioEntity);
}
