package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.CasaRequest;
import guardian.dto.admin.CasaResponse;
import guardian.dto.admin.ImportacionCasasResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.CasaService;
import guardian.service.admin.ImportacionCasasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(ApiEndpoint.ADMIN_CASAS)
@RequiredArgsConstructor
public class CasaController {

    private final CasaService casaService;
    private final ImportacionCasasService importacionCasasService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<CasaResponse>> listar() {
        return ResponseEntity.ok(casaService.listar(usuarioActual.conjuntoId()));
    }

    @PostMapping
    public ResponseEntity<CasaResponse> crear(@Valid @RequestBody CasaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(casaService.crear(request, usuarioActual.obtener()));
    }

    @PutMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<CasaResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody CasaRequest request) {
        return ResponseEntity.ok(casaService.actualizar(id, request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<CasaResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(casaService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<CasaResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(casaService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    // ── Carga masiva ─────────────────────────────────────────────────────────

    /**
     * La plantilla, generada con las MISMAS columnas que lee el importador y
     * con los tipos de vivienda REALES de esta sede: una plantilla que muestra
     * un tipo que el conjunto no tiene ensena a fallar.
     */
    @GetMapping(ApiEndpoint.PLANTILLA)
    public ResponseEntity<byte[]> plantilla() {
        byte[] libro = importacionCasasService.plantilla(usuarioActual.conjuntoId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plantilla-casas.xlsx\"")
                .body(libro);
    }

    @PostMapping(ApiEndpoint.IMPORTAR)
    public ResponseEntity<ImportacionCasasResponse> importar(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(
                importacionCasasService.importar(archivo, usuarioActual.obtener()));
    }
}
