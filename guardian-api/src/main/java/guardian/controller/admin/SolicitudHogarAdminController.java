package guardian.controller.admin;

import guardian.dto.admin.SolicitudHogarAdminResponse;
import guardian.constant.ApiEndpoint;
import guardian.security.UsuarioActual;
import guardian.service.admin.SolicitudHogarAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Bandeja de solicitudes de union al hogar por codigo del titular.
 *
 * <p>Aprobar es lo que de verdad crea la persona y su cuenta de acceso, y por
 * eso es del administrador y no del formulario publico que las recibe.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_SOLICITUDES_HOGAR)
@RequiredArgsConstructor
public class SolicitudHogarAdminController {

    private final SolicitudHogarAdminService solicitudHogarAdminService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<SolicitudHogarAdminResponse>> pendientes() {
        return ResponseEntity.ok(
                solicitudHogarAdminService.pendientes(usuarioActual.conjuntoId()));
    }

    @PatchMapping(ApiEndpoint.APROBAR)
    public ResponseEntity<SolicitudHogarAdminResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(
                solicitudHogarAdminService.aprobar(id, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RECHAZAR)
    public ResponseEntity<SolicitudHogarAdminResponse> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> cuerpo) {

        String motivo = cuerpo == null ? null : cuerpo.get("motivo");
        return ResponseEntity.ok(
                solicitudHogarAdminService.rechazar(id, motivo, usuarioActual.obtener()));
    }
}
