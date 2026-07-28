package guardian.dto.acceso;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerificarQrRequest {

    /** Contenido crudo del QR escaneado. */
    @NotBlank(message = "No se leyo ningun codigo")
    private String payload;

    /** Porteria por donde se esta atendiendo. Opcional si el conjunto tiene una sola. */
    private Long puntoAccesoId;
}
