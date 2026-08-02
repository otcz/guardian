package guardian.controller.publico;

import guardian.constant.ApiEndpoint;
import guardian.dto.auth.RestablecerClaveRequest;
import guardian.dto.auth.SolicitarCodigoRequest;
import guardian.dto.auth.SolicitudCodigoResponse;
import guardian.service.auth.RecuperacionClaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * "Olvide mi contrasena". Sin sesion: es el unico camino de vuelta cuando
 * nadie puede entrar a pedirlo desde adentro.
 */
@RestController
@RequestMapping(ApiEndpoint.PUBLICO_RECUPERACION)
@RequiredArgsConstructor
public class RecuperacionController {

    private final RecuperacionClaveService recuperacionClaveService;

    /** Siempre 200, exista o no la cuenta. Ver SolicitudCodigoResponse. */
    @PostMapping("/solicitar")
    public ResponseEntity<SolicitudCodigoResponse> solicitar(
            @Valid @RequestBody SolicitarCodigoRequest request) {
        return ResponseEntity.ok(recuperacionClaveService.solicitar(request));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(
            @Valid @RequestBody RestablecerClaveRequest request) {
        recuperacionClaveService.restablecer(request);
        return ResponseEntity.noContent().build();
    }
}
