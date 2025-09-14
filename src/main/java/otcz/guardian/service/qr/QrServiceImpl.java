package otcz.guardian.service.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import otcz.guardian.DTO.qr.QrPayloadDTO;
import otcz.guardian.DTO.qr.QrTokenDTO;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.qr.ValidacionQrResponse;
import otcz.guardian.utils.JwtUtil;
import java.util.Date;

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
    public ResponseEntity<?> generarQr(String documentoNumero, String placa) {
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
        response.setUsuario("ENCONTRADO");
        String usuarioEstado = usuario.getEstado() != null ? usuario.getEstado().name() : "DESCONOCIDO";
        if (!EstadoUsuario.ACTIVO.equals(usuario.getEstado())) {
            response.setVehiculo("NO_VALIDADO");
            response.setMensaje("USUARIO INACTIVO");
            response.setQrBase64(null);
            return ResponseEntity.badRequest().body(response);
        }
        Vehiculo vehiculo = null;
        String vehiculoEstado = "NO TIENE VEHICULO AUTORIZADO";
        Long vehiculoId = null;
        if (placa != null && !placa.trim().isEmpty()) {
            Optional<Vehiculo> vehiculoOpt = vehiculoRepository.findByPlaca(placa);
            if (vehiculoOpt.isPresent()) {
                vehiculo = vehiculoOpt.get();
                vehiculoId = vehiculo.getId();
                if (!vehiculo.getActivo()) {
                    vehiculoEstado = "INACTIVO";
                } else if (!vehiculo.getUsuarioEntity().getId().equals(usuario.getId())) {
                    vehiculoEstado = "NO ASIGNADO AL USUARIO";
                } else {
                    vehiculoEstado = "ACTIVO";
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
}
