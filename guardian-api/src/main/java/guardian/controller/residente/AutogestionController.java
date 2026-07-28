package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.VehiculoResponse;
import guardian.dto.residente.FamiliarRequest;
import guardian.dto.residente.FamiliarResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.security.UsuarioActual;
import guardian.service.admin.PersonaRegistrada;
import guardian.service.residente.AutogestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * "Mi hogar": familia y vehiculos del residente en sesion. Todo opera sobre SU
 * casa; los ids ajenos rebotan en el service.
 */
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE)
@RequiredArgsConstructor
public class AutogestionController {

    private final AutogestionService autogestionService;
    private final UsuarioActual usuarioActual;

    // ── Familia ──────────────────────────────────────────────────────────────

    @GetMapping(ApiEndpoint.RESIDENTE_FAMILIA)
    public ResponseEntity<List<FamiliarResponse>> familia() {
        return ResponseEntity.ok(autogestionService.listarFamilia(usuarioActual.obtener()));
    }

    @PostMapping(ApiEndpoint.RESIDENTE_FAMILIA)
    public ResponseEntity<PersonaRegistrada> agregarFamiliar(
            @Valid @RequestBody FamiliarRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(autogestionService.agregarFamiliar(request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RESIDENTE_FAMILIA + "/{personaId}/activar")
    public ResponseEntity<FamiliarResponse> activarFamiliar(@PathVariable Long personaId) {
        return ResponseEntity.ok(
                autogestionService.cambiarEstadoFamiliar(personaId, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RESIDENTE_FAMILIA + "/{personaId}/desactivar")
    public ResponseEntity<FamiliarResponse> desactivarFamiliar(@PathVariable Long personaId) {
        return ResponseEntity.ok(
                autogestionService.cambiarEstadoFamiliar(personaId, false, usuarioActual.obtener()));
    }

    // ── Vehiculos ────────────────────────────────────────────────────────────

    @GetMapping(ApiEndpoint.RESIDENTE_VEHICULOS)
    public ResponseEntity<List<VehiculoResponse>> vehiculos() {
        return ResponseEntity.ok(autogestionService.listarMisVehiculos(usuarioActual.obtener()));
    }

    @PostMapping(ApiEndpoint.RESIDENTE_VEHICULOS)
    public ResponseEntity<VehiculoResponse> agregarVehiculo(
            @Valid @RequestBody VehiculoResidenteRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(autogestionService.agregarVehiculo(request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RESIDENTE_VEHICULOS + "/{id}/activar")
    public ResponseEntity<VehiculoResponse> activarVehiculo(@PathVariable Long id) {
        return ResponseEntity.ok(
                autogestionService.cambiarEstadoVehiculo(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.RESIDENTE_VEHICULOS + "/{id}/desactivar")
    public ResponseEntity<VehiculoResponse> desactivarVehiculo(@PathVariable Long id) {
        return ResponseEntity.ok(
                autogestionService.cambiarEstadoVehiculo(id, false, usuarioActual.obtener()));
    }
}
