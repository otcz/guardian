package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.AsignarGuardiasRequest;
import guardian.dto.admin.GuardiaPorteriaResponse;
import guardian.dto.admin.PuntoAccesoRequest;
import guardian.dto.admin.PuntoAccesoResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.GuardiaPorteriaService;
import guardian.service.admin.PuntoAccesoService;
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
 * Porterias del conjunto. Sin DELETE a proposito: la bitacora apunta a la
 * porteria por la que paso cada persona, asi que se desactiva, no se borra.
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN_PORTERIAS)
@RequiredArgsConstructor
public class PuntoAccesoController {

    private final PuntoAccesoService puntoAccesoService;
    private final GuardiaPorteriaService guardiaPorteriaService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<PuntoAccesoResponse>> listar() {
        return ResponseEntity.ok(puntoAccesoService.listar(usuarioActual.conjuntoId()));
    }

    @PostMapping
    public ResponseEntity<PuntoAccesoResponse> crear(
            @Valid @RequestBody PuntoAccesoRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(puntoAccesoService.crear(request, usuarioActual.obtener()));
    }

    @PutMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<PuntoAccesoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody PuntoAccesoRequest request) {
        return ResponseEntity.ok(
                puntoAccesoService.actualizar(id, request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<PuntoAccesoResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(
                puntoAccesoService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<PuntoAccesoResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(
                puntoAccesoService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    // ── Guardias asignados ───────────────────────────────────────────────────

    @GetMapping(ApiEndpoint.GUARDIAS)
    public ResponseEntity<List<GuardiaPorteriaResponse>> guardias(@PathVariable Long id) {
        return ResponseEntity.ok(guardiaPorteriaService.listar(id, usuarioActual.obtener()));
    }

    /**
     * Reemplazo total: quedan EXACTAMENTE los que vengan. Con altas y bajas
     * sueltas, dos administradores editando a la vez dejan un estado que
     * ninguno de los dos eligio.
     */
    @PutMapping(ApiEndpoint.GUARDIAS)
    public ResponseEntity<PuntoAccesoResponse> asignarGuardias(
            @PathVariable Long id, @Valid @RequestBody AsignarGuardiasRequest request) {
        return ResponseEntity.ok(
                guardiaPorteriaService.asignar(id, request, usuarioActual.obtener()));
    }
}
