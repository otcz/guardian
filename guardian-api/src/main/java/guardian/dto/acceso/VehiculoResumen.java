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

    /**
     * La foto del carro. Cumple el mismo papel que la de la persona: marca y
     * color describen, pero no identifican —en un conjunto hay varios "Mazda
     * gris"—, y la placa se lee mal de noche o viene tapada de barro.
     *
     * <p>Null cuando el vehiculo no tiene foto cargada, que es un estado
     * legitimo: al carro lo identifica su placa, no su foto.</p>
     */
    private String fotoUrl;
}
