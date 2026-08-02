package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

/**
 * Vehiculo tal como aparece en el boton que toca el guardia.
 */
@Data
@Builder
public class VehiculoResumen {

    private Long id;
    private String placa;

    /**
     * Ya traducidos del catalogo. En la garita no hay tiempo para que el
     * cliente resuelva codigos: lo que llega es lo que se pinta.
     */
    private String tipo;
    private String marca;
    private String color;
}
