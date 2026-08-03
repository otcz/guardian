package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Paso 2: el codigo que llego al correo y el PIN nuevo. */
@Data
public class RestablecerClaveRequest {

    @NotBlank(message = "Escribe tu numero de documento")
    @Size(max = 20)
    private String documento;

    /**
     * SEIS digitos, a diferencia del PIN que son cuatro. La distincion es
     * deliberada: en la misma pantalla conviven los dos campos, y con la misma
     * longitud la gente escribiria su PIN donde va el codigo.
     */
    @NotBlank(message = "Escribe el codigo que te llego")
    @Size(max = 12)
    private String codigo;

    // Mismas reglas que el cambio normal: recuperar no puede ser la puerta
    // trasera para ponerse un PIN que el cambio normal rechazaria.
    @NotBlank(message = "Escribe el PIN nuevo")
    @Pattern(regexp = "\\d{4}", message = "El PIN son 4 numeros")
    private String claveNueva;
}
