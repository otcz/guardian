package otcz.guardian.controller.vehiculo;

import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.service.vehiculo.VehiculoService;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;

    public VehiculoController(VehiculoService vehiculoService, UsuarioService usuarioService) {
        this.vehiculoService = vehiculoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<VehiculoEntity> registrarVehiculo(@RequestBody VehiculoEntity vehiculoEntity) {
        return ResponseEntity.ok(vehiculoService.registrarVehiculo(vehiculoEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculoEntity> actualizarVehiculo(@PathVariable Long id, @RequestBody VehiculoEntity vehiculoEntity) {
        vehiculoEntity.setId(id);
        return ResponseEntity.ok(vehiculoService.actualizarVehiculo(vehiculoEntity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Long id) {
        vehiculoService.eliminarVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{placa}")
    public ResponseEntity<VehiculoEntity> obtenerPorPlaca(@PathVariable String placa) {
        return vehiculoService.obtenerPorPlaca(placa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<VehiculoEntity>> listarVehiculosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(vehiculoService.listarVehiculosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<VehiculoEntity>> listarVehiculosPorTipo(@PathVariable TipoVehiculo tipo) {
        return ResponseEntity.ok(vehiculoService.listarVehiculosPorTipo(tipo));
    }

    @GetMapping("/usuario/{usuarioId}/activos")
    public ResponseEntity<List<VehiculoEntity>> listarVehiculosActivosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(vehiculoService.listarVehiculosActivosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }
}
