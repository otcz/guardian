package otcz.guardian.controller.usuario.invitado;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.service.usuario.invitado.InvitadoService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invitados")
public class InvitadoController {

    private final InvitadoService invitadoService;
    private final UsuarioService usuarioService;

    public InvitadoController(InvitadoService invitadoService, UsuarioService usuarioService) {
        this.invitadoService = invitadoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Invitado> crearInvitado(@RequestBody Invitado invitado) {
        return ResponseEntity.ok(invitadoService.crearInvitado(invitado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invitado> actualizarInvitado(@PathVariable Long id, @RequestBody Invitado invitado) {
        invitado.setId(id);
        return ResponseEntity.ok(invitadoService.actualizarInvitado(invitado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInvitado(@PathVariable Long id) {
        invitadoService.eliminarInvitado(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/qr/{codigoQR}")
    public ResponseEntity<Invitado> obtenerPorCodigoQR(@PathVariable String codigoQR) {
        return invitadoService.obtenerPorCodigoQR(codigoQR)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Invitado>> listarInvitadosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(invitadoService.listarInvitadosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}/activos")
    public ResponseEntity<List<Invitado>> listarInvitadosActivosPorUsuario(@PathVariable Long usuarioId) {
        return usuarioService.obtenerUsuarioPorId(usuarioId)
                .map(u -> ResponseEntity.ok(invitadoService.listarInvitadosActivosPorUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/validos")
    public ResponseEntity<List<Invitado>> listarInvitadosValidos() {
        LocalDateTime ahora = LocalDateTime.now();
        return ResponseEntity.ok(invitadoService.listarInvitadosValidos(ahora));
    }
}
