package otcz.guardian.repository.vehiculo;

import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<VehiculoEntity, Long> {

    // Buscar vehículo por placa
    Optional<VehiculoEntity> findByPlaca(String placa);

    // Listar vehículos de un usuario
    List<VehiculoEntity> findByUsuarioEntity(UsuarioEntity usuarioEntity);

    // Listar vehículos por tipo
    List<VehiculoEntity> findByTipo(TipoVehiculo tipo);

    // Listar vehículos activos de un usuario
    List<VehiculoEntity> findByUsuarioEntityAndActivoTrue(UsuarioEntity usuarioEntity);
}
