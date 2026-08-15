package guardian.controller.acceso;

import guardian.constant.ApiEndpoint;
import guardian.dto.acceso.FiltroEventos;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.CandidatoGaritaResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.PorteriasGaritaResponse;
import guardian.dto.acceso.PresenciaResponse;
import guardian.dto.acceso.QuienEstaResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarDocumentoRequest;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

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
    private final guardian.service.acceso.BusquedaGaritaService busquedaGaritaService;
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

    /**
     * Respaldo del escaneo: buscar a alguien por su nombre.
     *
     * <p>GET y no POST porque no cambia nada, y sin el nombre en el path para
     * que no quede escrito en los registros de acceso del proxy: un nombre
     * propio en una URL viaja a los logs de Cloudflare y de Caddy.</p>
     */
    @GetMapping(ApiEndpoint.ACCESO_BUSCAR)
    public ResponseEntity<List<CandidatoGaritaResponse>> buscar(
            @RequestParam(name = "q", required = false) String texto) {

        return ResponseEntity.ok(
                busquedaGaritaService.buscar(texto, usuarioActual.obtener()));
    }

    /** Contadores ADENTRO / AFUERA que encabezan la pantalla de la porteria. */
    @GetMapping("/presencia")
    public ResponseEntity<PresenciaResponse> presencia() {
        return ResponseEntity.ok(presenciaService.conteo(usuarioActual.conjuntoId()));
    }

    /**
     * QUIENES estan adentro o afuera, no cuantos.
     *
     * <p>El contador responde "cuantos"; esto responde "quien falta por salir",
     * que es la pregunta de verdad a la hora de cerrar o de evacuar.</p>
     */
    @GetMapping("/presencia/personas")
    public ResponseEntity<List<QuienEstaResponse>> quienesEstan(
            @RequestParam(defaultValue = "E") String sentido) {

        return ResponseEntity.ok(
                presenciaService.quienesEstan(usuarioActual.conjuntoId(), sentido));
    }

    /** Paso 1: el guardia escanea y ve la ficha con la foto. */
    @PostMapping(ApiEndpoint.ACCESO_VERIFICAR)
    public ResponseEntity<FichaVerificacionResponse> verificar(
            @Valid @RequestBody VerificarQrRequest request) {
        return ResponseEntity.ok(accesoService.verificar(request, usuarioActual.obtener()));
    }

    /**
     * Paso 1, por el otro camino: identificar por documento.
     *
     * <p>Lo usan el lector de codigo de barras y la cedula tecleada, y devuelve
     * la MISMA ficha que el QR: lo que cambia es como se llego a ella. Tambien
     * responde por un invitado que llega sin su codigo y entrega la cedula.</p>
     */
    @PostMapping(ApiEndpoint.ACCESO_VERIFICAR_DOCUMENTO)
    public ResponseEntity<FichaVerificacionResponse> verificarPorDocumento(
            @Valid @RequestBody VerificarDocumentoRequest request) {
        return ResponseEntity.ok(
                accesoService.verificarPorDocumento(request, usuarioActual.obtener()));
    }

    /** Paso 2: el guardia confirma a pie o con placa. */
    @PostMapping(ApiEndpoint.ACCESO_REGISTRAR)
    public ResponseEntity<AccesoEventoResponse> registrar(
            @Valid @RequestBody RegistrarAccesoRequest request) {
        return ResponseEntity.ok(accesoService.registrar(request, usuarioActual.obtener()));
    }

    @GetMapping(ApiEndpoint.ACCESO_EVENTOS)
    public ResponseEntity<Page<AccesoEventoResponse>> eventos(
            @ModelAttribute FiltroEventos filtros,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {

        return ResponseEntity.ok(accesoService.buscarEventos(
                usuarioActual.conjuntoId(), filtros, PageRequest.of(pagina, tamano)));
    }
}
