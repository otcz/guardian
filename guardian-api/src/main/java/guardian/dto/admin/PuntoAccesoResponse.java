package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PuntoAccesoResponse {

    private Long id;
    private String nombre;
    private String direccion;
    private String permiteVehiculo;
    private String activo;

    /**
     * Cuantos pasos se han registrado por aca.
     *
     * <p>Se muestra antes de deshabilitar: una porteria con miles de registros
     * es la principal del conjunto, y apagarla por equivocacion se nota en la
     * garita, no en esta pantalla.</p>
     */
    private long registros;

    /**
     * Cuantos guardias tiene asignados. Un numero y no la lista: la tabla solo
     * necesita el conteo, y traer los nombres de cada porteria en el listado
     * seria una consulta mas por fila para un dato que solo se lee al abrir la
     * hoja de asignacion.
     */
    private long guardias;
}
