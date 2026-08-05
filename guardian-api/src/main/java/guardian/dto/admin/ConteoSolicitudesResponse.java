package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * Cuantas decisiones tiene esperando el administrador, por tipo y en total.
 *
 * <p>El total es el numero del aviso del menu, y el desglose es para que la
 * bandeja pueda decir cuantas hay de cada clase sin pedir las dos listas
 * completas solo para contarlas.</p>
 */
@Data
@Builder
public class ConteoSolicitudesResponse {

    private long casas;
    private long vehiculos;
    private long pendientes;
}
