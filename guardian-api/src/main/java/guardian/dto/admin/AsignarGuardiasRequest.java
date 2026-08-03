package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * El conjunto COMPLETO de guardias que quedan en la porteria.
 *
 * <p>Reemplazo total y no un alta/baja por fila: la hoja de la pantalla se
 * guarda de una sola vez, y con operaciones sueltas dos administradores
 * editando a la vez dejan un estado que ninguno de los dos eligio.</p>
 */
@Data
public class AsignarGuardiasRequest {

    /** Vacia es valida: significa "esta porteria se queda sin guardias asignados". */
    @NotNull(message = "Falta la lista de guardias")
    private List<Long> personaIds;
}
