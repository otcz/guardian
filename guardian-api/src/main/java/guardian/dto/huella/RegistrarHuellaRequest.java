package guardian.dto.huella;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Las lecturas de UN dedo, para guardarlo.
 *
 * <p>Las lecturas llegan en base64 porque son binarias y viajan en JSON. No son
 * tres huellas: son tres vistas del mismo dedo que el algoritmo funde en una
 * sola plantilla.</p>
 */
@Data
public class RegistrarHuellaRequest {

    @NotNull(message = "Falta la persona")
    private Long personaId;

    @NotBlank(message = "Falta el dedo")
    private String dedo;

    @NotEmpty(message = "Faltan las lecturas del dedo")
    private List<String> lecturas;

    /** Lo que reporto el lector, de 0 a 100. Sirve para diagnosticar despues. */
    private Integer calidad;
}
