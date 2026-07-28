package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UsuarioRequest {

    @NotNull(message = "Selecciona la persona")
    private Long personaId;

    /** Codigo del grupo ROL: ADMIN, GUARDIA o RESIDENTE. */
    @NotBlank(message = "Selecciona el rol")
    private String rol;
}
