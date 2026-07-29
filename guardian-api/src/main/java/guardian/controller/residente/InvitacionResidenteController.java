package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.invitacion.InvitacionRequest;
import guardian.dto.invitacion.InvitacionResponse;
import guardian.security.UsuarioActual;
import guardian.service.acceso.InvitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Invitaciones de MI casa. No hay eliminar: una invitacion comprometida se
 * revoca, y el historial de sus ingresos queda en la bitacora.
 */
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE + ApiEndpoint.RESIDENTE_INVITACIONES)
@RequiredArgsConstructor
public class InvitacionResidenteController {

    private final InvitacionService invitacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<InvitacionResponse>> listar() {
        return ResponseEntity.ok(invitacionService.listarDeMiCasa(usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<InvitacionResponse> crear(@Valid @RequestBody InvitacionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(invitacionService.crear(request, usuarioActual.obtener()));
    }

    @PatchMapping("/{id}/revocar")
    public ResponseEntity<InvitacionResponse> revocar(@PathVariable Long id) {
        return ResponseEntity.ok(invitacionService.revocar(id, usuarioActual.obtener()));
    }
}
