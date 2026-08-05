package guardian.dto.residente;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SolicitudCasaRequest {

    @NotNull(message = "Elige tu casa")
    private Long casaId;

    /** Codigo del grupo PARENTESCO. */
    @NotBlank(message = "Elige tu parentesco")
    private String parentesco;
}
