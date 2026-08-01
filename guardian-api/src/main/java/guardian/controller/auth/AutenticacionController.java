package guardian.controller.auth;

import guardian.constant.ApiEndpoint;
import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.security.UsuarioActual;
import guardian.service.auth.AutenticacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping(ApiEndpoint.AUTH)
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;
    private final UsuarioActual usuarioActual;

    @PostMapping(ApiEndpoint.AUTH_LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticacionService.login(request));
    }

    /** Devuelve una sesion nueva: el token anterior quedo degradado a CLAVE_PENDIENTE. */
    @PostMapping(ApiEndpoint.AUTH_CAMBIAR_CLAVE)
    public ResponseEntity<LoginResponse> cambiarClave(
            @Valid @RequestBody CambiarClaveRequest request) {
        return ResponseEntity.ok(
                autenticacionService.cambiarClave(usuarioActual.obtener(), request));
    }

    @GetMapping(ApiEndpoint.AUTH_YO)
    public ResponseEntity<SesionResponse> yo() {
        return ResponseEntity.ok(autenticacionService.sesionActual(usuarioActual.obtener()));
    }
}
