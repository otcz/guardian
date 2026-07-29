package guardian.controller.publico;

import guardian.constant.ApiEndpoint;
import guardian.dto.invitacion.InvitacionPublicaResponse;
import guardian.service.acceso.InvitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pagina que abre el INVITADO desde el link que le compartieron por
 * WhatsApp. Sin cuenta ni sesion: el codigo UUID no adivinable es la llave,
 * el mismo modelo de los links de rastreo de paqueteria.
 */
@RestController
@RequestMapping(ApiEndpoint.PUBLICO_INVITACIONES)
@RequiredArgsConstructor
public class InvitacionPublicaController {

    private final InvitacionService invitacionService;

    @GetMapping("/{codigoPublico}")
    public ResponseEntity<InvitacionPublicaResponse> ver(@PathVariable String codigoPublico) {
        return ResponseEntity.ok(invitacionService.publica(codigoPublico));
    }
}
