package otcz.guardian.repository.vehiculo;

import otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculoUsuarioRepository extends JpaRepository<VehiculoUsuarioEntity, PK> {
    List<VehiculoUsuarioEntity> findByVehiculo_Id(Long vehiculoId);
    List<VehiculoUsuarioEntity> findByUsuario_Id(Long usuarioId);
    List<VehiculoUsuarioEntity> findByVehiculo_IdAndUsuario_Id(Long vehiculoId, Long usuarioId);
}

