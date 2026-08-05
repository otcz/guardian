package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

/**
 * Una casa como la ve alguien que todavia no vive en ninguna.
 *
 * <p>Deliberadamente SIN nombres de residentes: quien elige casa aun no
 * pertenece a ninguna, y una lista con "CASA-101 — familia Perez" le entregaria
 * el directorio del conjunto a cualquiera con cuenta.</p>
 */
@Data
@Builder
public class CasaDisponibleResponse {

    private Long id;
    private String identificador;

    /**
     * Si la casa ya tiene titular. Lo unico que hace falta saber de los demas:
     * dice si entraria como titular o a una familia que ya existe, y por lo
     * tanto que parentesco puede elegir.
     */
    private boolean tieneTitular;
}
