package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CambiarClaveRequest {

    @NotBlank(message = "Escribe tu contrasena actual")
    private String claveActual;

    @NotBlank(message = "Escribe la nueva contrasena")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String claveNueva;
}
