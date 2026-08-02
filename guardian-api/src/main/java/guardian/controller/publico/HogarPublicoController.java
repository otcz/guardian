package guardian.controller.publico;

import guardian.constant.ApiEndpoint;
import guardian.dto.residente.HogarPublicoResponse;
import guardian.dto.residente.RegistroHogarRequest;
import guardian.service.residente.CodigoHogarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * La pantalla que abre quien recibio el codigo de su familia. Sin cuenta ni
 * sesion: el UUID no adivinable es la llave, igual que el link del invitado.
 *
 * <p>La diferencia con aquel es seria y por eso este codigo es de un solo uso
 * y con vencimiento: aquel deja pasar a alguien una vez, este CREA una persona
 * dentro del conjunto, con credencial propia.</p>
 */
@RestController
@RequestMapping(ApiEndpoint.PUBLICO_HOGAR)
@RequiredArgsConstructor
public class HogarPublicoController {

    private final CodigoHogarService codigoHogarService;

    @GetMapping("/{codigo}")
    public ResponseEntity<HogarPublicoResponse> ver(@PathVariable String codigo) {
        return ResponseEntity.ok(codigoHogarService.consultar(codigo));
    }

    @PostMapping("/{codigo}")
    public ResponseEntity<Void> registrar(@PathVariable String codigo,
                                          @Valid @RequestBody RegistroHogarRequest request) {
        codigoHogarService.registrar(codigo, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
