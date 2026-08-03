package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class PuntoAccesoRequest {

    @NotBlank(message = "Escribe el nombre de la porteria")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String nombre;

    @Size(max = 160, message = "La direccion no puede superar 160 caracteres")
    private String direccion;

    /**
     * "S"/"N" por convencion del proyecto. Nulo se toma como "S": el caso comun
     * es una porteria por donde entra todo, y obligar a marcarlo agregaria un
     * paso al alta mas frecuente.
     */
    @Pattern(regexp = "S|N", message = "Valor no valido")
    private String permiteVehiculo;

    private String observaciones;
}
