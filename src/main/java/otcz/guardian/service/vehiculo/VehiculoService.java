package otcz.guardian.service.vehiculo;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.utils.TipoVehiculo;
import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    Vehiculo registrarVehiculo(Vehiculo vehiculo);

    Vehiculo actualizarVehiculo(Vehiculo vehiculo);

    void eliminarVehiculo(Long id);

    Optional<Vehiculo> obtenerPorPlaca(String placa);

    List<Vehiculo> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity);

    List<Vehiculo> listarVehiculosPorTipo(TipoVehiculo tipo);

    List<Vehiculo> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity);
}
