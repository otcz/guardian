package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.GrupoParametroResponse;
import guardian.dto.admin.ParametroRequest;
import guardian.dto.common.ParametroResponse;
import guardian.service.admin.ParametroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_PARAMETROS)
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroService parametroService;

    @GetMapping
    public ResponseEntity<List<ParametroResponse>> listar(@RequestParam String grupo) {
        return ResponseEntity.ok(parametroService.listarPorGrupo(grupo));
    }

    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoParametroResponse>> grupos() {
        return ResponseEntity.ok(parametroService.listarGrupos());
    }

    @GetMapping("/grupos/{grupo}")
    public ResponseEntity<List<ParametroResponse>> administrarGrupo(@PathVariable String grupo) {
        return ResponseEntity.ok(parametroService.administrarGrupo(grupo));
    }

    @PostMapping("/grupos/{grupo}")
    public ResponseEntity<ParametroResponse> crear(@PathVariable String grupo,
                                                   @Valid @RequestBody ParametroRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parametroService.crear(grupo, solicitud));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParametroResponse> renombrar(@PathVariable Long id,
                                                       @Valid @RequestBody ParametroRequest solicitud) {
        return ResponseEntity.ok(parametroService.renombrar(id, solicitud));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ParametroResponse> cambiarEstado(@PathVariable Long id,
                                                           @RequestParam boolean activo) {
        return ResponseEntity.ok(parametroService.cambiarEstado(id, activo));
    }
}
