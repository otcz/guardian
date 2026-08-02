package guardian.controller.admin;

import guardian.constant.ApiEndpoint;
import guardian.dto.admin.ResumenResponse;
import guardian.security.UsuarioActual;
import guardian.service.admin.ResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoint.ADMIN_RESUMEN)
@RequiredArgsConstructor
public class ResumenController {

    private final ResumenService resumenService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<ResumenResponse> resumen() {
        return ResponseEntity.ok(resumenService.resumen(usuarioActual.obtener()));
    }
}
