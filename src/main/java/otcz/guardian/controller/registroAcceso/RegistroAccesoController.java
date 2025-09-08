package otcz.guardian.controller.registroAcceso;

import otcz.guardian.entity.registroAcceso.RegistroAcceso;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import otcz.guardian.service.registroAcceso.RegistroAccesoService;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.service.usuario.invitado.InvitadoService;
import otcz.guardian.service.vehiculo.VehiculoService;
import otcz.guardian.utils.ResultadoAcceso;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/registros")
public class RegistroAccesoController {

    private final RegistroAccesoService registroAccesoService;
    private final UsuarioService usuarioService;
    private final InvitadoService invitadoService;
    private final VehiculoService vehiculoService;

    public RegistroAccesoController(RegistroAccesoService registroAccesoService,
                                    UsuarioService usuarioService,
                                    InvitadoService invitadoService,
                                    VehiculoService vehiculoService) {
        this.registroAccesoService = registroAccesoService;
        this.usuarioService = usuarioService;
        this.invitadoService = invitadoService;
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    public ResponseEntity<RegistroAcceso> registrarAcceso(@RequestBody RegistroAcceso registroAcceso) {
        return ResponseEntity.ok(registroAccesoService.registrarAcceso(registroAcceso));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<RegistroAcceso>> historialPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(registroAccesoService.historialPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/invitado/{invitadoId}")
    public ResponseEntity<List<RegistroAcceso>> historialPorInvitado(@PathVariable Long invitadoId) {
        return invitadoService.obtenerPorCodigoQR(invitadoId.toString()) // opcional: buscar por ID real
                .map(i -> ResponseEntity.ok(registroAccesoService.historialPorInvitado(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public ResponseEntity<List<RegistroAcceso>> historialPorVehiculo(@PathVariable Long vehiculoId) {
        return vehiculoService.obtenerPorPlaca(vehiculoId.toString()) // opcional: buscar por placa real
                .map(v -> ResponseEntity.ok(registroAccesoService.historialPorVehiculo(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resultado/{resultado}")
    public ResponseEntity<List<RegistroAcceso>> historialPorResultado(@PathVariable ResultadoAcceso resultado) {
        return ResponseEntity.ok(registroAccesoService.historialPorResultado(resultado));
    }

    @GetMapping("/fechas")
    public ResponseEntity<List<RegistroAcceso>> historialEntreFechas(@RequestParam LocalDateTime inicio,
                                                                     @RequestParam LocalDateTime fin) {
        return ResponseEntity.ok(registroAccesoService.historialEntreFechas(inicio, fin));
    }
}