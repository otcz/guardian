package guardian.controller.acceso;

import guardian.constant.ApiEndpoint;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.PorteriasGaritaResponse;
import guardian.dto.acceso.PresenciaResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.security.UsuarioActual;
import guardian.service.acceso.AccesoService;
import guardian.service.acceso.PorteriaGaritaService;
import guardian.service.acceso.PresenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;

/**
 * Operacion de la porteria. Restringido a los roles GUARDIA y ADMIN en
 * {@link guardian.config.SecurityConfig}.
 */
@RestController
@RequestMapping(ApiEndpoint.ACCESO)
@RequiredArgsConstructor
public class AccesoController {

    private final AccesoService accesoService;
    private final PresenciaService presenciaService;
    private final PorteriaGaritaService porteriaGaritaService;
    private final UsuarioActual usuarioActual;

    /**
     * Entre que porterias puede elegir esta tablet, y cual proponerle.
     *
     * <p>Va aca y no bajo /admin porque quien la llama es el guardia, que no
     * tiene permiso de administracion.</p>
     */
    @GetMapping(ApiEndpoint.ACCESO_PORTERIAS)
    public ResponseEntity<PorteriasGaritaResponse> porterias() {
        return ResponseEntity.ok(porteriaGaritaService.disponibles(usuarioActual.obtener()));
    }

    /** Contadores ADENTRO / AFUERA que encabezan la pantalla de la porteria. */
    @GetMapping("/presencia")
    public ResponseEntity<PresenciaResponse> presencia() {
        return ResponseEntity.ok(presenciaService.conteo(usuarioActual.conjuntoId()));
    }

    /** Paso 1: el guardia escanea y ve la ficha con la foto. */
    @PostMapping(ApiEndpoint.ACCESO_VERIFICAR)
    public ResponseEntity<FichaVerificacionResponse> verificar(
            @Valid @RequestBody VerificarQrRequest request) {
        return ResponseEntity.ok(accesoService.verificar(request, usuarioActual.obtener()));
    }

    /** Paso 2: el guardia confirma a pie o con placa. */
    @PostMapping(ApiEndpoint.ACCESO_REGISTRAR)
    public ResponseEntity<AccesoEventoResponse> registrar(
            @Valid @RequestBody RegistrarAccesoRequest request) {
        return ResponseEntity.ok(accesoService.registrar(request, usuarioActual.obtener()));
    }

    @GetMapping(ApiEndpoint.ACCESO_EVENTOS)
    public ResponseEntity<Page<AccesoEventoResponse>> eventos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date hasta,
            @RequestParam(required = false) Long casaId,
            @RequestParam(required = false) String resultado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {

        return ResponseEntity.ok(accesoService.buscarEventos(
                usuarioActual.conjuntoId(), desde, hasta, casaId, resultado,
                PageRequest.of(pagina, tamano)));
    }
}
