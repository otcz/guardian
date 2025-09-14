package otcz.guardian.service.vehiculo;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.TipoVehiculo;
import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    VehiculoEntity registrarVehiculo(VehiculoEntity vehiculoEntity);

    VehiculoEntity actualizarVehiculo(VehiculoEntity vehiculoEntity);

    void eliminarVehiculo(Long id);

    Optional<VehiculoEntity> obtenerPorPlaca(String placa);

    List<VehiculoEntity> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity);

    List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo);

    List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity);
}
