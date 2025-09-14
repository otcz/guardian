package otcz.guardian.DTO.qr;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class QrPayloadDTO {
    private Long usuarioId;
    private Long vehiculoId;
    private LocalDateTime expiracion;
}

