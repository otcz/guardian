package guardian.controller.superadmin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.sede.SedeRequest;
import guardian.dto.sede.SedeResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.SedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Panel de la plataforma. Exclusivo del rol SUPER_ADMIN por
 * {@link guardian.config.SecurityConfig}.
 */
@RestController
@RequestMapping(ApiEndpoint.SUPER_SEDES)
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<SedeResponse>> listar() {
        return ResponseEntity.ok(sedeService.listar());
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sedeService.crear(request, usuarioActual.obtener()));
    }

    @PutMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<SedeResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody SedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizar(id, request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<SedeResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<SedeResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    /** El primer administrador de la sede. Nace activo: no hay quien lo habilite. */
    @PostMapping(ApiEndpoint.SEDE_ADMINISTRADOR)
    public ResponseEntity<SedeResponse> crearAdministrador(
            @PathVariable Long id, @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sedeService.crearAdministrador(id, request, usuarioActual.obtener()));
    }

    /**
     * Entra a administrar una sede. Devuelve una sesion NUEVA con la sede en
     * el token: a partir de ahi todo /api/admin opera sobre ella sin que
     * ningun endpoint tenga que aceptar la sede como parametro.
     */
    @PostMapping(ApiEndpoint.SEDE_ENTRAR)
    public ResponseEntity<LoginResponse> entrar(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.entrar(id, usuarioActual.obtener()));
    }

    @PostMapping(ApiEndpoint.SEDE_SALIR)
    public ResponseEntity<LoginResponse> salir() {
        return ResponseEntity.ok(sedeService.salir(usuarioActual.obtener()));
    }
}
