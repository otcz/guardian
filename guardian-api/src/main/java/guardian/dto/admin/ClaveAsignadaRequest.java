package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * PIN que la administracion le asigna a una cuenta.
 *
 * <p>Mismas reglas que el cambio propio: si el panel aceptara un PIN mas debil
 * que el que el sistema le exige a su dueno, la puerta ancha seria justamente
 * la que abre el administrador. Los triviales los rechaza ValidadorPin, que es
 * el mismo para los tres caminos.</p>
 */
@Data
public class ClaveAsignadaRequest {

    @NotBlank(message = "Escribe el PIN")
    @Pattern(regexp = "\\d{4}", message = "El PIN son 4 numeros")
    private String claveNueva;
}
