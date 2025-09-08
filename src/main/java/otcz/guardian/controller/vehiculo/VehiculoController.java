package otcz.guardian.controller.vehiculo;

import otcz.guardian.entity.vehiculo.Vehiculo;
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
    public ResponseEntity<Vehiculo> registrarVehiculo(@RequestBody Vehiculo vehiculo) {
        return ResponseEntity.ok(vehiculoService.registrarVehiculo(vehiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizarVehiculo(@PathVariable Long id, @RequestBody Vehiculo vehiculo) {
        vehiculo.setId(id);
        return ResponseEntity.ok(vehiculoService.actualizarVehiculo(vehiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Long id) {
        vehiculoService.eliminarVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{placa}")
    public ResponseEntity<Vehiculo> obtenerPorPlaca(@PathVariable String placa) {
        return vehiculoService.obtenerPorPlaca(placa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Vehiculo>> listarVehiculosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(vehiculoService.listarVehiculosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Vehiculo>> listarVehiculosPorTipo(@PathVariable TipoVehiculo tipo) {
        return ResponseEntity.ok(vehiculoService.listarVehiculosPorTipo(tipo));
    }

    @GetMapping("/usuario/{usuarioId}/activos")
    public ResponseEntity<List<Vehiculo>> listarVehiculosActivosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(vehiculoService.listarVehiculosActivosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }
}
