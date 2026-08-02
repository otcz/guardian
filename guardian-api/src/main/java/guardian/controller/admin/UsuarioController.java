package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.ClaveAsignadaRequest;
import guardian.dto.admin.UsuarioRequest;
import guardian.dto.admin.UsuarioResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_USUARIOS)
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar(usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioService.crear(request, usuarioActual.obtener()));
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id,
                                                      @RequestParam String rol) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, rol, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    @PatchMapping("/{id}/restablecer-clave")
    public ResponseEntity<UsuarioResponse> restablecerClave(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.restablecerClave(id, usuarioActual.obtener()));
    }

    /** Clave elegida por la administracion, para cuando restablecer no alcanza. */
    @PatchMapping("/{id}/clave")
    public ResponseEntity<UsuarioResponse> asignarClave(
            @PathVariable Long id, @Valid @RequestBody ClaveAsignadaRequest request) {

        return ResponseEntity.ok(usuarioService.asignarClave(
                id, request.getClaveNueva(), usuarioActual.obtener()));
    }
}
