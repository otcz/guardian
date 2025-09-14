package otcz.guardian.service.qr;

import otcz.guardian.DTO.qr.QrPayloadDTO;


public interface QrService {
    String generarQr(QrPayloadDTO payload); // Devuelve el QR en base64
    QrPayloadDTO decodificarQr(String qrData);
    boolean isQrExpirado(QrPayloadDTO payload);
    // Genera y valida QR a partir de documento y placa
    org.springframework.http.ResponseEntity<?> generarQr(String documentoNumero, String placa);
}
