package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

/**
 * Conteo de presencia que encabeza la pantalla de la porteria.
 */
@Data
@Builder
public class PresenciaResponse {

    /** Personas cuyo ultimo movimiento permitido fue una ENTRADA. */
    private long adentro;

    /** Personas activas del conjunto que no estan adentro. */
    private long afuera;

    private long totalActivos;
}
