package guardian.dto.residente;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * La foto que el residente sube para su propia credencial.
 *
 * <p>Solo la URL: el archivo ya viajo por /api/fotos, que es quien valida
 * tamano y formato. Aca se comprueba que la ruta sea de una subida propia y
 * no un enlace externo.</p>
 */
@Data
public class MiFotoRequest {

    @NotBlank(message = "Falta la foto")
    @Size(max = 500)
    private String fotoUrl;
}
