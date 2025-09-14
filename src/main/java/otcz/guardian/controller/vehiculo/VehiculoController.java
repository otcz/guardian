package otcz.guardian.controller.vehiculo;

import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.service.vehiculo.VehiculoService;
import otcz.guardian.utils.ApiEndpoints;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(ApiEndpoints.Vehiculo.BASE)
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;

    public VehiculoController(VehiculoService vehiculoService, UsuarioService usuarioService) {
        this.vehiculoService = vehiculoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping(ApiEndpoints.Vehiculo.CREAR)
    public ResponseEntity<?> registrarVehiculo(@RequestBody VehiculoEntity vehiculoEntity) {
        VehiculoEntity creado = vehiculoService.registrarVehiculo(vehiculoEntity);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_CREADO);
    }

    @PutMapping(ApiEndpoints.Vehiculo.MODIFICAR)
    public ResponseEntity<?> actualizarVehiculo(@PathVariable Long id, @RequestBody VehiculoEntity vehiculoEntity) {
        if (!vehiculoService.obtenerPorId(id).isPresent()) {
            return ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        }
        vehiculoEntity.setId(id);
        vehiculoService.actualizarVehiculo(vehiculoEntity);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_MODIFICADO);
    }

    @DeleteMapping(ApiEndpoints.Vehiculo.ELIMINAR)
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long id) {
        if (!vehiculoService.obtenerPorId(id).isPresent()) {
            return ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        }
        vehiculoService.eliminarVehiculo(id);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_ELIMINADO);
    }

    @GetMapping(ApiEndpoints.Vehiculo.POR_PLACA)
    public ResponseEntity<?> obtenerPorPlaca(@PathVariable String placa) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        java.util.Optional<VehiculoEntity> vehiculoOpt = vehiculoService.obtenerPorPlaca(placa);
        return vehiculoOpt.isPresent() ? ResponseEntity.ok(vehiculoOpt.get()) : notFound;
    }

    @GetMapping(ApiEndpoints.Vehiculo.POR_USUARIO)
    public ResponseEntity<?> listarVehiculosPorUsuario(@PathVariable Long usuarioId) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.USUARIO_NO_ENCONTRADO);
        Optional<?> usuarioOpt = usuarioService.obtenerUsuarioPorId(usuarioId);
        if (usuarioOpt.isPresent()) {
            List<VehiculoEntity> lista = vehiculoService.listarVehiculosPorUsuario((UsuarioEntity) usuarioOpt.get());
            return ResponseEntity.ok(lista);
        } else {
            return notFound;
        }
    }

    @GetMapping(ApiEndpoints.Vehiculo.POR_TIPO)
    public ResponseEntity<?> listarVehiculosPorTipo(@PathVariable TipoVehiculo tipo) {
        List<VehiculoEntity> lista = vehiculoService.listarVehiculosPorTipo(tipo);
        return ResponseEntity.ok(lista);
    }

    @GetMapping(ApiEndpoints.Vehiculo.ACTIVOS_POR_USUARIO)
    public ResponseEntity<?> listarVehiculosActivosPorUsuario(@PathVariable Long usuarioId) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.USUARIO_NO_ENCONTRADO);
        Optional<?> usuarioOpt = usuarioService.obtenerUsuarioPorId(usuarioId);
        if (usuarioOpt.isPresent()) {
            List<VehiculoEntity> lista = vehiculoService.listarVehiculosActivosPorUsuario((UsuarioEntity) usuarioOpt.get());
            return ResponseEntity.ok(lista);
        } else {
            return notFound;
        }
    }
}
