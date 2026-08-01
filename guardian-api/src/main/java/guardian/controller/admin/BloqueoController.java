package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.BloqueoRequest;
import guardian.security.UsuarioActual;
import guardian.service.admin.BloqueoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Bloqueos de la administracion. Exclusivo del rol ADMIN por
 * {@link guardian.config.SecurityConfig}.
 *
 * <p>POST bloquea (con motivo), DELETE desbloquea. Se agrupan en un recurso
 * propio y no como una accion mas de cada panel para que quede claro que son
 * la OTRA llave: activar/desactivar es del dueno, bloquear es del conjunto.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_BLOQUEOS)
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService bloqueoService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/personas/{id}")
    public ResponseEntity<Void> bloquearPersona(@PathVariable Long id,
                                                @Valid @RequestBody BloqueoRequest request) {
        bloqueoService.bloquearPersona(id, request.getMotivo(), usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/personas/{id}")
    public ResponseEntity<Void> desbloquearPersona(@PathVariable Long id) {
        bloqueoService.desbloquearPersona(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vehiculos/{id}")
    public ResponseEntity<Void> bloquearVehiculo(@PathVariable Long id,
                                                 @Valid @RequestBody BloqueoRequest request) {
        bloqueoService.bloquearVehiculo(id, request.getMotivo(), usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vehiculos/{id}")
    public ResponseEntity<Void> desbloquearVehiculo(@PathVariable Long id) {
        bloqueoService.desbloquearVehiculo(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/casas/{id}")
    public ResponseEntity<Void> bloquearCasa(@PathVariable Long id,
                                             @Valid @RequestBody BloqueoRequest request) {
        bloqueoService.bloquearCasa(id, request.getMotivo(), usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/casas/{id}")
    public ResponseEntity<Void> desbloquearCasa(@PathVariable Long id) {
        bloqueoService.desbloquearCasa(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    /** Corta el acceso de un guardia, y su sesion viva con el. */
    @PostMapping("/usuarios/{id}")
    public ResponseEntity<Void> bloquearUsuario(@PathVariable Long id,
                                                @Valid @RequestBody BloqueoRequest request) {
        bloqueoService.bloquearUsuario(id, request.getMotivo(), usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> desbloquearUsuario(@PathVariable Long id) {
        bloqueoService.desbloquearUsuario(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }
}
