package guardian.dto.huella;

import lombok.Builder;
import lombok.Data;

/** Si el modulo de huella opera. Lo consulta la pantalla al abrirse. */
@Data
@Builder
public class EstadoHuellaResponse {

    /** false mientras no haya lector: la pantalla dice "sensor no conectado". */
    private boolean disponible;

    /** "ZKFinger V10.0", "SourceAFIS 3.x" o "NINGUNO". */
    private String algoritmo;

    private int maximoDedos;
    private int capturasPorDedo;
}
