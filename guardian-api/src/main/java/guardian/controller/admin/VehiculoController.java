package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.ImportacionVehiculosResponse;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.ImportacionVehiculosService;
import guardian.service.admin.VehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_VEHICULOS)
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final ImportacionVehiculosService importacionVehiculosService;
    private final UsuarioActual usuarioActual;

    // ── Carga masiva ─────────────────────────────────────────────────────────

    /**
     * La plantilla, con las MISMAS columnas que lee el importador y una segunda
     * hoja con las casas, tipos, marcas y colores reales de esta sede.
     */
    @GetMapping(ApiEndpoint.PLANTILLA)
    public ResponseEntity<byte[]> plantilla() {
        byte[] libro = importacionVehiculosService.plantilla(usuarioActual.conjuntoId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plantilla-vehiculos.xlsx\"")
                .body(libro);
    }

    @PostMapping(ApiEndpoint.IMPORTAR)
    public ResponseEntity<ImportacionVehiculosResponse> importar(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(
                importacionVehiculosService.importar(archivo, usuarioActual.obtener()));
    }

    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listar(
            @RequestParam(required = false) Long casaId) {

        Long conjuntoId = usuarioActual.conjuntoId();
        return ResponseEntity.ok(casaId == null
                ? vehiculoService.listar(conjuntoId)
                : vehiculoService.listarPorCasa(casaId, conjuntoId));
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vehiculoService.crear(request, usuarioActual.obtener()));
    }

    @PutMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<VehiculoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.ok(vehiculoService.actualizar(id, request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<VehiculoResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<VehiculoResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    /** Eliminacion fisica. Solo el super administrador: ver Autoridad.exigirSuperAdmin. */
    @DeleteMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }
}
