package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.common.ParametroResponse;
import guardian.service.admin.ParametroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
