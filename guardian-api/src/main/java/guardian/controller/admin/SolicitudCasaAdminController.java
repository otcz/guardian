package guardian.controller.admin;

import guardian.dto.admin.SolicitudCasaAdminResponse;
import guardian.constant.ApiEndpoint;
import guardian.security.UsuarioActual;
import guardian.service.admin.SolicitudCasaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Bandeja de solicitudes de casa.
 *
 * <p>Aprobar es lo que de verdad mete a alguien en un hogar — con sus
 * vehiculos, sus invitaciones y su familia detras—, y por eso es del
 * administrador y no del residente que lo pide.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_SOLICITUDES_CASA)
@RequiredArgsConstructor
public class SolicitudCasaAdminController {

    private final SolicitudCasaAdminService solicitudCasaAdminService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<SolicitudCasaAdminResponse>> pendientes() {
        return ResponseEntity.ok(
                solicitudCasaAdminService.pendientes(usuarioActual.conjuntoId()));
    }

    /**
     * Solo el numero, para el aviso del menu.
     *
     * <p>Endpoint aparte y no la lista completa: esto se pide en CADA
     * navegacion del panel, y traer las solicitudes enteras para pintar un
     * numerito seria pagar la consulta grande todo el tiempo.</p>
     */
    @GetMapping(ApiEndpoint.CONTEO)
    public ResponseEntity<Map<String, Long>> conteo() {
        return ResponseEntity.ok(Collections.singletonMap("pendientes",
                solicitudCasaAdminService.cuantasPendientes(usuarioActual.conjuntoId())));
    }

    @PatchMapping(ApiEndpoint.APROBAR)
    public ResponseEntity<SolicitudCasaAdminResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(
                solicitudCasaAdminService.aprobar(id, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RECHAZAR)
    public ResponseEntity<SolicitudCasaAdminResponse> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> cuerpo) {

        String motivo = cuerpo == null ? null : cuerpo.get("motivo");
        return ResponseEntity.ok(
                solicitudCasaAdminService.rechazar(id, motivo, usuarioActual.obtener()));
    }
}
