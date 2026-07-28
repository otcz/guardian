package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

@Data
public class CasaRequest {

    @Size(max = 20, message = "La torre no puede superar 20 caracteres")
    private String torre;

    @NotBlank(message = "Escribe el numero de la casa")
    @Size(max = 20, message = "El numero no puede superar 20 caracteres")
    private String numero;

    @PositiveOrZero(message = "Los cupos no pueden ser negativos")
    private Integer cuposParqueadero;

    private String observaciones;
}
