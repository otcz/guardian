package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Lo que la tablet necesita para saber en que porteria esta.
 *
 * <p>Un objeto y no una lista pelada: ademas de las opciones hace falta cual
 * proponer, y el default se calcula en el servidor —donde vive la asignacion—
 * para que no existan dos reglas, una en Java y otra en TypeScript.</p>
 */
@Data
@Builder
public class PorteriasGaritaResponse {

    private List<PorteriaGaritaResponse> porterias;

    /**
     * La que se le propone al guardia, o null si no hay una clara.
     *
     * <p>Es una SUGERENCIA, no una imposicion: el guardia puede elegir otra y
     * queda registrado. Bloquearlo dejaria gente esperando en la puerta.</p>
     */
    private Long sugeridaId;
}
