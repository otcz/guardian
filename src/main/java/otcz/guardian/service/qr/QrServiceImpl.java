package otcz.guardian.service.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import otcz.guardian.DTO.qr.*;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;

@Service
public class QrServiceImpl implements QrService {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public String generarQr(QrPayloadDTO payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            BufferedImage qrImage = createQRImage(json, 300, 300);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR", e);
        }
    }

    @Override
    public QrPayloadDTO decodificarQr(String qrData) {
        try {
            // qrData es el string JSON decodificado del QR
            return objectMapper.readValue(qrData, QrPayloadDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error decodificando QR", e);
        }
    }

    @Override
    public boolean isQrExpirado(QrPayloadDTO payload) {
        return payload.getExpiracion().isBefore(LocalDateTime.now());
    }

    @Override
    public ResponseEntity<?> generarQr(String documentoNumero, String placa, String estadoUsuario) {
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByDocumentoNumero(documentoNumero);
        ValidacionQrResponse response = new ValidacionQrResponse();
        if (!usuarioOpt.isPresent()) {
            response.setUsuario("NO_ENCONTRADO");
            response.setVehiculo("NO_VALIDADO");
            response.setMensaje("USUARIO NO ENCONTRADO");
            response.setQrBase64(null);
            return ResponseEntity.badRequest().body(response);
        }
        UsuarioEntity usuario = usuarioOpt.get();
        response.setUsuario(estadoUsuario); // Aquí se envía 'Usuario. Activo' o 'Usuario. Inactivo'
        String usuarioEstado = usuario.getEstado() != null ? usuario.getEstado().name() : "DESCONOCIDO";
        if (!EstadoUsuario.ACTIVO.equals(usuario.getEstado())) {
            response.setVehiculo("NO_VALIDADO");
            response.setMensaje("USUARIO INACTIVO");
            response.setQrBase64(null);
            return ResponseEntity.badRequest().body(response);
        }
        VehiculoEntity vehiculoEntity = null;
        String vehiculoEstado = "NO TIENE VEHICULO AUTORIZADO";
        Long vehiculoId = null;
        if (placa != null && !placa.trim().isEmpty()) {
            Optional<VehiculoEntity> vehiculoOpt = vehiculoRepository.findByPlaca(placa);
            if (vehiculoOpt.isPresent()) {
                vehiculoEntity = vehiculoOpt.get();
                vehiculoId = vehiculoEntity.getId();
                if (!vehiculoEntity.getActivo()) {
                    vehiculoEstado = "INACTIVO";
                } else {
                    // Verificar si el usuario está asociado al vehículo por la tabla intermedia
                    boolean asignado = false;
                    if (vehiculoEntity.getVehiculoUsuarios() != null) {
                        for (otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity rel : vehiculoEntity.getVehiculoUsuarios()) {
                            if (rel.getUsuario() != null && rel.getUsuario().getId().equals(usuario.getId())) {
                                asignado = true;
                                break;
                            }
                        }
                    }
                    if (!asignado) {
                        vehiculoEstado = "NO ASIGNADO AL USUARIO";
                    } else {
                        vehiculoEstado = "ACTIVO";
                    }
                }
            }
        }
        // Construir el token para el QR
        QrTokenDTO token = new QrTokenDTO();
        token.setUsuarioId(usuario.getId());
        token.setUsuarioEstado(usuarioEstado);
        token.setVehiculoId(vehiculoId);
        token.setVehiculoEstado(vehiculoEstado);
        token.setExpiracion(LocalDateTime.now().plusMinutes(15));
        String qrBase64 = generarQrToken(token);
        response.setQrBase64(qrBase64);
        response.setMensaje("QR GENERADO EXITOSAMENTE");
        response.setVehiculo(vehiculoEstado);
        return ResponseEntity.ok(response);
    }

    // Nuevo método para serializar el token y generar el QR como JWT
    public String generarQrToken(QrTokenDTO token) {
        try {
            Date expiracion = java.sql.Timestamp.valueOf(token.getExpiracion());
            String jwt = jwtUtil.generateQrToken(
                token.getUsuarioId(),
                token.getUsuarioEstado(),
                token.getVehiculoId(),
                token.getVehiculoEstado(),
                expiracion
            );
            BufferedImage qrImage = createQRImage(jwt, 300, 300);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR", e);
        }
    }

    private BufferedImage createQRImage(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    @Override
    public ResponseEntity<?> validarQr(QrTokenRequest request, Authentication authentication) {
        // Validación de autenticación del guardia
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
            Long vehiculoId = claims.get("vehiculoId", Long.class);
            Date expiracion = claims.getExpiration();

            // Validación de expiración del QR
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
                Optional<VehiculoEntity> vehiculoOpt = vehiculoRepository.findById(vehiculoId);
                if (!vehiculoOpt.isPresent()) {
                    // Buscar si el usuario tiene algún vehículo registrado usando la tabla intermedia
                    List<VehiculoEntity> vehiculosUsuario = new ArrayList<VehiculoEntity>();
                    if (usuario.getVehiculoUsuarios() != null) {
                        for (otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity rel : usuario.getVehiculoUsuarios()) {
                            if (rel.getVehiculo() != null) {
                                vehiculosUsuario.add(rel.getVehiculo());
                            }
                        }
                    }
                    if (vehiculosUsuario == null || vehiculosUsuario.isEmpty()) {
                        response.setTipoVehiculo(MensajeResponse.NO_VEHICULO);
                        response.setPlaca(MensajeResponse.NO_VEHICULO);
                        response.setEstadoVehiculo(MensajeResponse.NO_VEHICULO);
                    } else {
                        response.setTipoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                        response.setPlaca(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                        response.setEstadoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                    }
                    return ResponseEntity.ok(response);
                }
                VehiculoEntity vehiculoEntity = vehiculoOpt.get();
                response.setTipoVehiculo(vehiculoEntity.getTipo() != null ? vehiculoEntity.getTipo().name() : MensajeResponse.NO_VEHICULO);
                response.setPlaca(vehiculoEntity.getPlaca() != null ? vehiculoEntity.getPlaca() : MensajeResponse.NO_VEHICULO);
                response.setEstadoVehiculo(
                    vehiculoEntity.getActivo() != null ?
                        (vehiculoEntity.getActivo() ? otcz.guardian.utils.EstadoVehiculo.ACTIVO.name() : otcz.guardian.utils.EstadoVehiculo.INACTIVO.name())
                        : null
                );
                return ResponseEntity.ok(response);
            } else {
                // Si no hay vehiculoId, buscar si el usuario tiene algún vehículo registrado
                // Buscar si el usuario tiene algún vehículo registrado usando la tabla intermedia
                List<VehiculoEntity> vehiculosUsuario = new ArrayList<VehiculoEntity>();
                if (usuario.getVehiculoUsuarios() != null) {
                    for (otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity rel : usuario.getVehiculoUsuarios()) {
                        if (rel.getVehiculo() != null) {
                            vehiculosUsuario.add(rel.getVehiculo());
                        }
                    }
                }
                if (vehiculosUsuario == null || vehiculosUsuario.isEmpty()) {
                    response.setTipoVehiculo(MensajeResponse.NO_VEHICULO);
                    response.setPlaca(MensajeResponse.NO_VEHICULO);
                    response.setEstadoVehiculo(MensajeResponse.NO_VEHICULO);
                } else {
                    response.setTipoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                    response.setPlaca(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                    response.setEstadoVehiculo(MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
                }
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MensajeResponse.TOKEN_QR_INVALIDO);
        }
    }
}
