package otcz.guardian.controller.qr;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import otcz.guardian.utils.ApiEndpoints;
import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.qr.ValidacionQrResponse;
import otcz.guardian.DTO.qr.ValidacionQrDetalleResponse;
import otcz.guardian.DTO.qr.QrTokenRequest;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.service.qr.QrService;
import otcz.guardian.utils.EstadoVehiculo;
import otcz.guardian.utils.JwtUtil;
import io.jsonwebtoken.Claims;

import java.util.Optional;
import java.util.Date;
import java.util.List;

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
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(MensajeResponse.GUARDIA_NO_AUTENTICADO);
        }
        String username = authentication.getName();
        Optional<UsuarioEntity> guardiaOpt = usuarioRepository.findByCorreo(username);
        if (!guardiaOpt.isPresent()) {
            return ResponseEntity.status(401).body(MensajeResponse.GUARDIA_NO_ENCONTRADO);
        }
        try {
            String token = request.getToken();
            Claims claims = jwtUtil.decodeQrToken(token);
            Long usuarioId = claims.get("usuarioId", Long.class);
            String usuarioEstado = claims.get("usuarioEstado", String.class);
            Long vehiculoId = claims.get("vehiculoId", Long.class);
            String vehiculoEstado = claims.get("vehiculoEstado", String.class);
            Date expiracion = claims.getExpiration();
            if (expiracion == null || expiracion.before(new Date())) {
                return ResponseEntity.badRequest().body(MensajeResponse.QR_EXPIRADO);
            }
            Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (!usuarioOpt.isPresent()) {
                return ResponseEntity.badRequest().body(MensajeResponse.USUARIO_NO_ENCONTRADO);
            }
            UsuarioEntity usuario = usuarioOpt.get();
            ValidacionQrDetalleResponse response = new ValidacionQrDetalleResponse();
            response.setUsuario(usuario.getNombreCompleto());
            response.setDocumento(usuario.getDocumentoNumero());
            response.setEstadoUsuario(usuario.getEstado() != null ? usuario.getEstado().name() : null);
            response.setCasa(usuario.getCasa());
            if (vehiculoId != null) {
                Optional<Vehiculo> vehiculoOpt = vehiculoRepository.findById(vehiculoId);
                if (!vehiculoOpt.isPresent()) {
                    // Buscar si el usuario tiene algún vehículo registrado
                    List<Vehiculo> vehiculosUsuario = usuario.getVehiculos();
                    if (vehiculosUsuario == null || vehiculosUsuario.isEmpty()) {
                        response.setTipoVehiculo(MensajeResponse.NO_VEHICULO);
                        response.setPlaca(MensajeResponse.NO_VEHICULO);
                        response.setEstadoVehiculo(MensajeResponse.NO_VEHICULO);
                    } else {
                        response.setTipoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                        response.setPlaca(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                        response.setEstadoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                    }
                    return ResponseEntity.ok(response);
                }
                Vehiculo vehiculo = vehiculoOpt.get();
                response.setTipoVehiculo(vehiculo.getTipo() != null ? vehiculo.getTipo().name() : MensajeResponse.NO_VEHICULO);
                response.setPlaca(vehiculo.getPlaca() != null ? vehiculo.getPlaca() : MensajeResponse.NO_VEHICULO);
                response.setEstadoVehiculo(
                    vehiculo.getActivo() != null ?
                        (vehiculo.getActivo() ? EstadoVehiculo.ACTIVO.name() : EstadoVehiculo.INACTIVO.name())
                        : null
                );
                return ResponseEntity.ok(response);
            } else {
                // Si no hay vehiculoId, buscar si el usuario tiene algún vehículo registrado
                List<Vehiculo> vehiculosUsuario = usuario.getVehiculos();
                if (vehiculosUsuario == null || vehiculosUsuario.isEmpty()) {
                    response.setTipoVehiculo(MensajeResponse.NO_VEHICULO);
                    response.setPlaca(MensajeResponse.NO_VEHICULO);
                    response.setEstadoVehiculo(MensajeResponse.NO_VEHICULO);
                } else {
                    response.setTipoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                    response.setPlaca(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                    response.setEstadoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO);
                }
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MensajeResponse.TOKEN_QR_INVALIDO);
        }
    }
}
