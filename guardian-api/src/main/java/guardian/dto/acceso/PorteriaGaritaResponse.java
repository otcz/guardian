package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

/** Una porteria como la ve la tablet: lo justo para elegirla y mostrarla. */
@Data
@Builder
public class PorteriaGaritaResponse {

    private Long id;
    private String nombre;
    private String direccion;

    /** "S"/"N". Una puerta peatonal no deberia registrar placas. */
    private String permiteVehiculo;

    /** Si el guardia que pregunta esta asignado a esta porteria. */
    private boolean asignada;
}
