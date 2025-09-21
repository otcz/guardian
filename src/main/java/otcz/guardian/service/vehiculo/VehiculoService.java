package otcz.guardian.service.vehiculo;

import otcz.guardian.DTO.vehiculo.VehiculoConUsuarioResponseDTO;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.TipoVehiculo;
import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    VehiculoEntity registrarVehiculo(VehiculoEntity vehiculoEntity);

    VehiculoEntity actualizarVehiculo(VehiculoEntity vehiculoEntity);

    void eliminarVehiculo(Long id);

    void eliminarVehiculoPorPlaca(String placa);

    Optional<VehiculoEntity> obtenerPorPlaca(String placa);

    Optional<VehiculoEntity> obtenerPorId(Long id);

    List<VehiculoEntity> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity);

    List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo);

    List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity);

    VehiculoEntity asignarUsuario(Long vehiculoId, Long usuarioId);

    List<VehiculoEntity> listarTodos();

    List<VehiculoConUsuarioResponseDTO> listarVehiculosPorCorreoUsuario(String correo);
}
