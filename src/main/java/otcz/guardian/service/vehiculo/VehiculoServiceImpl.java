package otcz.guardian.service.vehiculo;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
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
    public VehiculoEntity registrarVehiculo(VehiculoEntity vehiculoEntity) {
        return vehiculoRepository.save(vehiculoEntity);
    }

    @Override
    public VehiculoEntity actualizarVehiculo(VehiculoEntity vehiculoEntity) {
        return vehiculoRepository.save(vehiculoEntity);
    }

    @Override
    public void eliminarVehiculo(Long id) {
        vehiculoRepository.deleteById(id);
    }

    @Override
    public Optional<VehiculoEntity> obtenerPorPlaca(String placa) {
        return vehiculoRepository.findByPlaca(placa);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity) {
        return vehiculoRepository.findByUsuarioEntity(usuarioEntity);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo) {
        return vehiculoRepository.findByTipo(tipo);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity) {
        return vehiculoRepository.findByUsuarioEntity(usuarioEntity);
    }
}
