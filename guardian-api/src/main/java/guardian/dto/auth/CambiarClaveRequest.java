package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CambiarClaveRequest {

    @NotBlank(message = "Escribe tu contrasena actual")
    private String claveActual;

    // Tope 72: BCrypt ignora en silencio todo byte despues del 72. Aceptar una
    // clave mas larga le prometeria al usuario una seguridad que no tiene.
    @NotBlank(message = "Escribe la nueva contrasena")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
