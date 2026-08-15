package guardian.dto.residente;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Alta de vehiculo hecha por el residente. Sin casaId: siempre es la suya.
 */
@Data
public class VehiculoResidenteRequest {

    @NotBlank(message = "Escribe la placa")
    @Size(max = 10)
    private String placa;

    /** Codigo del grupo TIPO_VEHICULO. */
    @NotBlank(message = "Selecciona el tipo de vehiculo")
    private String tipo;

    @Size(max = 50)
    private String marca;

    @Size(max = 30)
    private String color;

    /** Ruta de la foto ya subida por /api/fotos. Opcional. */
    @Size(max = 500)
    private String fotoUrl;
}
