package otcz.guardian.service.qr;

import org.springframework.http.ResponseEntity;
import otcz.guardian.DTO.qr.QrPayloadDTO;
import otcz.guardian.DTO.qr.QrTokenRequest;


public interface QrService {
    String generarQr(QrPayloadDTO payload);

    QrPayloadDTO decodificarQr(String qrData);

    boolean isQrExpirado(QrPayloadDTO payload);

    ResponseEntity<?> generarQr(String documentoNumero, String placa);

    ResponseEntity<?> validarQr(QrTokenRequest request, org.springframework.security.core.Authentication authentication);
}
