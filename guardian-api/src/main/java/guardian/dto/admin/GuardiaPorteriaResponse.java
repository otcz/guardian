package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * Una persona en la lista de guardias de una porteria.
 *
 * <p>Es UNA sola lista con banderas y no dos listas separadas de "asignados" y
 * "candidatos": la pantalla es una lista de chequeo, y partirla obligaria a
 * fusionarlas en el cliente para pintarla.</p>
 */
@Data
@Builder
public class GuardiaPorteriaResponse {

    private Long personaId;
    private String nombreCompleto;
    private String documento;

    /** Si hoy esta asignado a esta porteria. */
    private boolean asignado;

    /**
     * Si la persona sigue teniendo cuenta de guardia.
     *
     * <p>Puede ser false y estar asignada: a alguien le cambiaron el rol
     * despues de asignarlo. La fila tiene que seguir viendose para poder
     * quitarla — filtrarla la volveria invisible e imposible de corregir.</p>
     */
    private boolean esGuardia;
}
