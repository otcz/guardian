package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Paso 1 de la recuperacion: quien soy.
 *
 * <p>Se pide el DOCUMENTO y no el correo. Es el mismo dato con el que la
 * persona inicia sesion —el unico que tiene seguro en la cabeza— y no obliga a
 * recordar con cual de sus correos quedo registrada.</p>
 */
@Data
public class SolicitarCodigoRequest {

    @NotBlank(message = "Escribe tu numero de documento")
    @Size(max = 20)
    private String documento;
}
