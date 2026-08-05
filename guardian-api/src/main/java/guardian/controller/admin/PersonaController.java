package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.ImportacionPersonasResponse;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.PersonaResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.ImportacionPersonasService;
import guardian.service.admin.PersonaRegistrada;
import guardian.service.admin.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
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
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_PERSONAS)
@RequiredArgsConstructor
public class PersonaController {

    private static final int TAMANO_QR_PX = 512;

    private final PersonaService personaService;
    private final ImportacionPersonasService importacionPersonasService;
    private final UsuarioActual usuarioActual;

    // ── Carga masiva ─────────────────────────────────────────────────────────

    /** La plantilla, con las MISMAS columnas que lee el importador. */
    @GetMapping(ApiEndpoint.PLANTILLA)
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plantilla-personas.xlsx\"")
                .body(importacionPersonasService.plantilla());
    }

    @PostMapping(ApiEndpoint.IMPORTAR)
    public ResponseEntity<ImportacionPersonasResponse> importar(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(
                importacionPersonasService.importar(archivo, usuarioActual.obtener()));
    }

    @GetMapping
    public ResponseEntity<Page<PersonaResponse>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        return ResponseEntity.ok(personaService.buscar(
                usuarioActual.obtener(), texto, PageRequest.of(pagina, tamano)));
    }

    @GetMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<PersonaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.obtener(id, usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<PersonaRegistrada> crear(@Valid @RequestBody PersonaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(personaService.crear(request, usuarioActual.obtener()));
    }

    @PutMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<PersonaResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.actualizar(id, request, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.ACTIVAR)
    public ResponseEntity<PersonaResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.cambiarEstado(id, true, usuarioActual.obtener()));
    }

    @PatchMapping(ApiEndpoint.DESACTIVAR)
    public ResponseEntity<PersonaResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.cambiarEstado(id, false, usuarioActual.obtener()));
    }

    /** Eliminacion fisica. Solo el super administrador: ver Autoridad.exigirSuperAdmin. */
    @DeleteMapping(ApiEndpoint.POR_ID)
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(ApiEndpoint.CREDENCIAL)
    public ResponseEntity<Map<String, String>> emitirCredencial(@PathVariable Long id) {
        String payload = personaService.emitirCredencial(id, usuarioActual.obtener());
        return ResponseEntity.ok(Collections.singletonMap("payload", payload));
    }

    /** Freno de emergencia: revoca sin reemitir. El QR muere en el proximo escaneo. */
    @DeleteMapping(ApiEndpoint.CREDENCIAL)
    public ResponseEntity<Void> revocarCredencial(@PathVariable Long id) {
        personaService.revocarCredencial(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }

    /** PNG para imprimir el carnet de quien no usa smartphone. */
    @GetMapping(value = ApiEndpoint.CREDENCIAL_PNG, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> credencialPng(
            @PathVariable Long id,
            @RequestParam(defaultValue = "" + TAMANO_QR_PX) int tamano) {

        // Acotado: un tamano arbitrario renderiza un bitmap gigante y eso es
        // un OOM gratis para cualquiera con sesion.
        int tamanoPx = Math.max(128, Math.min(tamano, 1024));
        byte[] png = personaService.credencialPng(id, usuarioActual.obtener(), tamanoPx);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
