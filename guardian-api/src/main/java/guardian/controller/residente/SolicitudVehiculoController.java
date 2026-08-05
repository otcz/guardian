package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.residente.SolicitudVehiculoResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.security.UsuarioActual;
import guardian.service.residente.SolicitudVehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * El titular pide que le autoricen un vehiculo.
 *
 * <p>No crea el vehiculo: crea la solicitud. Registrar una placa es darle paso
 * al conjunto a un carro que el guardia no va a volver a discutir, y eso lo
 * autoriza la administracion.</p>
 */
@Validated
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE_SOLICITUDES_VEHICULO)
@RequiredArgsConstructor
public class SolicitudVehiculoController {

    private final SolicitudVehiculoService solicitudVehiculoService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<SolicitudVehiculoResponse>> misSolicitudes() {
        return ResponseEntity.ok(
                solicitudVehiculoService.misSolicitudes(usuarioActual.obtener()));
    }

    @PostMapping
    public ResponseEntity<SolicitudVehiculoResponse> solicitar(
            @Valid @RequestBody VehiculoResidenteRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudVehiculoService.solicitar(request, usuarioActual.obtener()));
    }

    /** Quita de la pantalla una solicitud rechazada que el residente ya leyo. */
    @PatchMapping(ApiEndpoint.DESCARTAR)
    public ResponseEntity<Void> descartar(@PathVariable Long id) {
        solicitudVehiculoService.descartar(id, usuarioActual.obtener());
        return ResponseEntity.noContent().build();
    }
}
