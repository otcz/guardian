package guardian.dto.auth;

import guardian.constant.Codigos;
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
    @Size(min = Codigos.CLAVE_LONGITUD_MINIMA, max = Codigos.CLAVE_LONGITUD_MAXIMA,
            message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
