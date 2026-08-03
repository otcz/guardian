package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Cambio del PIN propio.
 *
 * <p>El PIN ACTUAL se pide con {@code @NotBlank} y no con el patron de cuatro
 * digitos: quien todavia trae el inicial —o una cuenta migrada de la epoca de
 * las contrasenas— tiene que poder entrar aca a cambiarlo. Validarle la FORMA
 * al actual dejaria a esa persona sin ninguna manera de arreglarlo.</p>
 */
@Data
public class CambiarClaveRequest {

    @NotBlank(message = "Escribe tu PIN actual")
    private String claveActual;

    @NotBlank(message = "Escribe el PIN nuevo")
    @Pattern(regexp = "\\d{4}", message = "El PIN son 4 numeros")
    private String claveNueva;
}
