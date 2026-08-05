package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.residente.CasaDisponibleResponse;
import guardian.dto.residente.SolicitudCasaRequest;
import guardian.dto.residente.SolicitudCasaResponse;
import guardian.security.UsuarioActual;
import guardian.service.residente.SolicitudCasaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Lo que puede hacer alguien que todavia no vive en ninguna casa: verlas y
 * pedir entrar a una. La solicitud no asigna nada — la administracion decide.
 */
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE)
@RequiredArgsConstructor
public class SolicitudCasaController {

    private final SolicitudCasaService solicitudCasaService;
    private final UsuarioActual usuarioActual;

    @GetMapping(ApiEndpoint.RESIDENTE_CASAS_DISPONIBLES)
    public ResponseEntity<List<CasaDisponibleResponse>> casasDisponibles() {
        return ResponseEntity.ok(
                solicitudCasaService.casasDisponibles(usuarioActual.obtener()));
    }

    /** 204 cuando nunca ha pedido nada: no es un error, es que no hay que mostrar. */
    @GetMapping(ApiEndpoint.RESIDENTE_SOLICITUD_CASA)
    public ResponseEntity<SolicitudCasaResponse> miSolicitud() {
        return solicitudCasaService.miSolicitud(usuarioActual.obtener())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(ApiEndpoint.RESIDENTE_SOLICITUD_CASA)
    public ResponseEntity<SolicitudCasaResponse> solicitar(
            @Valid @RequestBody SolicitudCasaRequest request) {
        return ResponseEntity.ok(
                solicitudCasaService.solicitar(request, usuarioActual.obtener()));
    }
}
