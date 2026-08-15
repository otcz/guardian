package guardian.controller.acceso;

import guardian.constant.ApiEndpoint;
import guardian.dto.huella.EstadoHuellaResponse;
import guardian.dto.huella.HuellasDeUnaPersonaResponse;
import guardian.dto.huella.RegistrarHuellaRequest;
import guardian.security.UsuarioActual;
import guardian.service.huella.HuellaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Enrolamiento de huellas desde la porteria.
 *
 * <p>Bajo /acceso y no bajo /admin porque quien enrola es el guardia, que tiene
 * a la persona enfrente poniendo el dedo. Hereda de ahi el permiso: GUARDIA o
 * ADMIN, nunca el super administrador ni el residente.</p>
 *
 * <p>La identificacion por huella NO esta aca: cuando exista, entra por el
 * mismo /acceso/verificar que ya resuelve el codigo y el documento, porque la
 * decision de dejar pasar tiene que salir de un solo sitio.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ACCESO + ApiEndpoint.ACCESO_HUELLAS)
@RequiredArgsConstructor
public class HuellaController {

    private final HuellaService huellaService;
    private final UsuarioActual usuarioActual;

    /** Si hay lector y con que algoritmo. La pantalla lo pregunta al abrirse. */
    @GetMapping("/estado")
    public ResponseEntity<EstadoHuellaResponse> estado() {
        return ResponseEntity.ok(huellaService.estado());
    }

    @GetMapping("/{personaId}")
    public ResponseEntity<HuellasDeUnaPersonaResponse> dePersona(
            @PathVariable Long personaId) {

        return ResponseEntity.ok(
                huellaService.dePersona(personaId, usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<HuellasDeUnaPersonaResponse> registrar(
            @Valid @RequestBody RegistrarHuellaRequest request) {

        return ResponseEntity.ok(
                huellaService.registrar(request, usuarioActual.obtener()));
    }

    @DeleteMapping("/{personaId}/{dedo}")
    public ResponseEntity<HuellasDeUnaPersonaResponse> eliminar(
            @PathVariable Long personaId, @PathVariable String dedo) {

        return ResponseEntity.ok(
                huellaService.eliminar(personaId, dedo, usuarioActual.obtener()));
    }
}
