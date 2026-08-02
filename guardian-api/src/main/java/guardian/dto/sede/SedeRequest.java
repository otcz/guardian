package guardian.dto.sede;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SedeRequest {

    @NotBlank(message = "Escribe el nombre de la sede")
    @Size(max = 150)
    private String nombre;

    @Size(max = 200)
    private String direccion;
}
