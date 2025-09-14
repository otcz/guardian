package otcz.guardian.controller.qr;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import otcz.guardian.utils.ApiEndpoints;
import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.qr.QrTokenRequest;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.service.qr.QrService;
import otcz.guardian.utils.JwtUtil;

import java.util.Optional;

@RestController
@RequestMapping(ApiEndpoints.Qr.BASE)
public class QrController {

    private final QrService qrService;
    private final UsuarioRepository usuarioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final JwtUtil jwtUtil;

    public QrController(QrService qrService, UsuarioRepository usuarioRepository, VehiculoRepository vehiculoRepository, JwtUtil jwtUtil) {
        this.qrService = qrService;
        this.usuarioRepository = usuarioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(ApiEndpoints.Qr.GENERAR)
    public ResponseEntity<?> generarQr(Authentication authentication,
                                       @RequestParam(required = false) String placa) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(MensajeResponse.USUARIO_NO_AUTENTICADO);
        }
        String username = authentication.getName();
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByCorreo(username);
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.status(401).body(MensajeResponse.USUARIO_NO_ENCONTRADO);
        }
        UsuarioEntity usuario = usuarioOpt.get();
        String documentoNumero = usuario.getDocumentoNumero();
        if (documentoNumero == null) {
            return ResponseEntity.badRequest().body(MensajeResponse.USUARIO_NO_TIENE_DOCUMENTO);
        }
        return qrService.generarQr(documentoNumero, placa);
    }

    @PostMapping(ApiEndpoints.Qr.VALIDAR)
    public ResponseEntity<?> validarQr(@RequestBody QrTokenRequest request,
                                       Authentication authentication) {
        return qrService.validarQr(request, authentication);
    }
}
