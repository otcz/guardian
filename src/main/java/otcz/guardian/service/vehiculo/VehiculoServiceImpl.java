package otcz.guardian.service.vehiculo;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @Override
    public Vehiculo registrarVehiculo(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    @Override
    public Vehiculo actualizarVehiculo(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    @Override
    public void eliminarVehiculo(Long id) {
        vehiculoRepository.deleteById(id);
    }

    @Override
    public Optional<Vehiculo> obtenerPorPlaca(String placa) {
        return vehiculoRepository.findByPlaca(placa);
    }

    @Override
    public List<Vehiculo> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity) {
        return vehiculoRepository.findByUsuarioEntity(usuarioEntity);
    }

    @Override
    public List<Vehiculo> listarVehiculosPorTipo(TipoVehiculo tipo) {
        return vehiculoRepository.findByTipo(tipo);
    }

    @Override
    public List<Vehiculo> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity) {
        return vehiculoRepository.findByUsuarioEntity(usuarioEntity);
    }
}
