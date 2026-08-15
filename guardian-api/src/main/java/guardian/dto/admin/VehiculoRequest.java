package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class VehiculoRequest {

    @NotNull(message = "Selecciona la casa")
    private Long casaId;

    @NotBlank(message = "Escribe la placa")
    @Size(max = 10, message = "La placa no puede superar 10 caracteres")
    private String placa;

    /** Codigo del grupo TIPO_VEHICULO. */
    @NotBlank(message = "Selecciona el tipo de vehiculo")
    private String tipo;

    @Size(max = 50)
    private String marca;

    @Size(max = 30)
    private String color;

    /**
     * Ruta de la foto ya subida por /api/fotos. Opcional: un vehiculo sin foto
     * es un estado legitimo — lo identifica la placa.
     */
    @Size(max = 500)
    private String fotoUrl;
}
