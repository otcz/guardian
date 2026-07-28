package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.CasaRequest;
import guardian.dto.admin.CasaResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.CasaService;
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

@RestController
@RequestMapping(ApiEndpoint.ADMIN_CASAS)
@RequiredArgsConstructor
public class CasaController {

    private final CasaService casaService;
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
}
