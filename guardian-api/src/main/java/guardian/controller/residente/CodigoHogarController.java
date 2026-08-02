package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.residente.CodigoHogarResponse;
import guardian.security.UsuarioActual;
import guardian.service.residente.CodigoHogarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El codigo con el que el titular invita a su familia a registrarse sola.
 *
 * <p>La casa sale del token: no hay id en la URL y por lo tanto no hay forma
 * de generar un codigo para el hogar de otro.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE + ApiEndpoint.RESIDENTE_CODIGO_HOGAR)
@RequiredArgsConstructor
public class CodigoHogarController {

    private final CodigoHogarService codigoHogarService;
    private final UsuarioActual usuarioActual;

    /** Null cuando el hogar nunca ha generado uno: la pantalla ofrece crearlo. */
    @GetMapping
    public ResponseEntity<CodigoHogarResponse> vigente() {
        return ResponseEntity.ok(codigoHogarService.vigente(usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<CodigoHogarResponse> generar() {
        return ResponseEntity.ok(codigoHogarService.generar(usuarioActual.obtener()));
    }

    @DeleteMapping
    public ResponseEntity<Void> revocar() {
        codigoHogarService.revocar(usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }
}
