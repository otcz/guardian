package otcz.guardian.DTO.qr;

import lombok.Data;

@Data
public class ValidacionQrDetalleResponse {
    private String usuario;
    private String documento;
    private String estadoUsuario;
    private String casa;
    private String tipoVehiculo;
    private String placa;
    private String estadoVehiculo;
}

