package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.invitacion.InvitacionResponse;
import guardian.security.UsuarioActual;
import guardian.service.acceso.InvitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_INVITACIONES)
@RequiredArgsConstructor
public class InvitacionAdminController {

    private final InvitacionService invitacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<InvitacionResponse>> listar() {
        return ResponseEntity.ok(invitacionService.listarDelConjunto(usuarioActual.conjuntoId()));
    }

    @PatchMapping("/{id}/revocar")
    public ResponseEntity<InvitacionResponse> revocar(@PathVariable Long id) {
        return ResponseEntity.ok(invitacionService.revocarComoAdmin(id, usuarioActual.obtener()));
    }

    /** Borrado fisico. El service exige super administrador. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        invitacionService.eliminarComoSuperAdmin(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }
}
