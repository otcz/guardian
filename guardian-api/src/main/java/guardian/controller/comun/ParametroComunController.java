package guardian.controller.comun;

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

/**
 * Catalogos de solo lectura para cualquier usuario autenticado. Los
 * formularios del residente (parentesco del familiar, tipo de vehiculo)
 * necesitan estos valores igual que el back-office; la administracion de los
 * catalogos sigue siendo exclusiva de /admin.
 */
@RestController
@RequestMapping(ApiEndpoint.PARAMETROS)
@RequiredArgsConstructor
public class ParametroComunController {

    private final ParametroService parametroService;

    @GetMapping
    public ResponseEntity<List<ParametroResponse>> listar(@RequestParam String grupo) {
        return ResponseEntity.ok(parametroService.listarPorGrupo(grupo));
    }
}
