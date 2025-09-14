package otcz.guardian.DTO.qr;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QrTokenDTO {
    private Long usuarioId;
    private String usuarioEstado;
    private Long vehiculoId;
    private String vehiculoEstado;
    private LocalDateTime expiracion;
}

