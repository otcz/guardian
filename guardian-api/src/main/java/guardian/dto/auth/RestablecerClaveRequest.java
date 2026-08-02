package guardian.dto.auth;

import guardian.constant.Codigos;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Paso 2: el codigo que llego al correo y la contrasena nueva. */
@Data
public class RestablecerClaveRequest {

    @NotBlank(message = "Escribe tu numero de documento")
    @Size(max = 20)
    private String documento;

    @NotBlank(message = "Escribe el codigo que te llego")
    @Size(max = 12)
    private String codigo;

    // Mismas reglas que el cambio normal: recuperar no puede ser la puerta
    // trasera para ponerse una clave que el cambio de clave rechazaria.
    @NotBlank(message = "Escribe la nueva contrasena")
    @Size(min = Codigos.CLAVE_LONGITUD_MINIMA, max = Codigos.CLAVE_LONGITUD_MAXIMA,
            message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
