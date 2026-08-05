package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.ConteoSolicitudesResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.SolicitudesResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que las dos bandejas tienen en comun: cuantas decisiones hay esperando.
 *
 * <p>Endpoint aparte y no las listas completas: esto se pide en CADA navegacion
 * del panel, y traer las solicitudes enteras para pintar un numerito seria
 * pagar la consulta grande todo el tiempo.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_SOLICITUDES)
@RequiredArgsConstructor
public class SolicitudesController {

    private final SolicitudesResumenService solicitudesResumenService;
    private final UsuarioActual usuarioActual;

    @GetMapping(ApiEndpoint.CONTEO)
    public ResponseEntity<ConteoSolicitudesResponse> conteo() {
        return ResponseEntity.ok(
                solicitudesResumenService.conteo(usuarioActual.conjuntoId()));
    }
}
