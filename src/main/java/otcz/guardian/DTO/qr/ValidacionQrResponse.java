package otcz.guardian.DTO.qr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionQrResponse {
    private String usuario;
    private String vehiculo;
    private String mensaje;
    private String qrBase64;
}
