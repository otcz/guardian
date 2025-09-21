package otcz.guardian.service.vehiculo;

import otcz.guardian.DTO.vehiculo.VehiculoConUsuarioResponseDTO;
import otcz.guardian.DTO.vehiculo.VehiculoRegistroDTO;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.TipoVehiculo;
import java.util.List;
import java.util.Optional;

public interface VehiculoService {

    VehiculoEntity registrarVehiculo(VehiculoRegistroDTO vehiculoRegistroDTO);

    VehiculoEntity actualizarVehiculo(VehiculoEntity vehiculoEntity);

    void eliminarVehiculo(Long id);

    void eliminarVehiculoPorPlaca(String placa);

    Optional<VehiculoEntity> obtenerPorPlaca(String placa);

    Optional<VehiculoEntity> obtenerPorId(Long id);

    List<VehiculoEntity> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity);

    List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo);

    List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity);

    /**
     * Asigna un usuario a un vehículo (tabla intermedia).
     * Lanza excepción si ya existe la relación o si no existen los IDs.
     */
    void asignarUsuarioAVehiculo(Long vehiculoId, Long usuarioId);

    List<VehiculoEntity> listarTodos();

    List<VehiculoConUsuarioResponseDTO> listarVehiculosPorCorreoUsuario(String correo);
}
