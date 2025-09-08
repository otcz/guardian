package otcz.guardian.service.vehiculo;

import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.utils.TipoVehiculo;
import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    Vehiculo registrarVehiculo(Vehiculo vehiculo);

    Vehiculo actualizarVehiculo(Vehiculo vehiculo);

    void eliminarVehiculo(Long id);

    Optional<Vehiculo> obtenerPorPlaca(String placa);

    List<Vehiculo> listarVehiculosPorUsuario(Usuario usuario);

    List<Vehiculo> listarVehiculosPorTipo(TipoVehiculo tipo);

    List<Vehiculo> listarVehiculosActivosPorUsuario(Usuario usuario);
}
