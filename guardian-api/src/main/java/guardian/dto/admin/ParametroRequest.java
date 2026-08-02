package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Alta o edicion de una opcion del catalogo.
 *
 * <p>Solo el texto visible: el CODIGO lo deriva el servidor. Pedirselo al
 * administrador seria pedirle que invente una llave tecnica, y de ahi salen
 * los "MARCA_1" y los codigos con tilde que despues nadie puede referenciar.</p>
 */
@Data
public class ParametroRequest {

    @NotBlank(message = "Escribe el nombre")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String valor;
}
