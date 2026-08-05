package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.SolicitudVehiculoAdminResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.SolicitudVehiculoAdminService;
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
 * Bandeja de vehiculos por autorizar.
 *
 * <p>Aprobar aca es lo que hace que la talanquera suba para esa placa. Por eso
 * la decision es del administrador y no del titular que la pide.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_SOLICITUDES_VEHICULO)
@RequiredArgsConstructor
public class SolicitudVehiculoAdminController {

    private final SolicitudVehiculoAdminService solicitudVehiculoAdminService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<SolicitudVehiculoAdminResponse>> pendientes() {
        return ResponseEntity.ok(
                solicitudVehiculoAdminService.pendientes(usuarioActual.conjuntoId()));
    }

    @PatchMapping(ApiEndpoint.APROBAR)
    public ResponseEntity<SolicitudVehiculoAdminResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(
                solicitudVehiculoAdminService.aprobar(id, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RECHAZAR)
    public ResponseEntity<SolicitudVehiculoAdminResponse> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> cuerpo) {

        String motivo = cuerpo == null ? null : cuerpo.get("motivo");
        return ResponseEntity.ok(
                solicitudVehiculoAdminService.rechazar(id, motivo, usuarioActual.obtener()));
    }
}
