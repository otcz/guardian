package guardian.dto.huella;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Que dedos tiene registrados una persona.
 *
 * <p>NO viaja la plantilla, y es a proposito: es un dato biometrico y no tiene
 * por que salir del servidor. La pantalla solo necesita saber cuales hay para
 * no ofrecer el mismo dedo dos veces.</p>
 */
@Data
@Builder
public class HuellasDeUnaPersonaResponse {

    private Long personaId;
    private String nombreCompleto;
    private List<DedoRegistrado> dedos;

    /** Si todavia puede registrar otro. */
    private boolean puedeAgregar;

    @Data
    @Builder
    public static class DedoRegistrado {
        private String dedo;
        private Integer calidad;
        private Date fechaRegistro;
        /** Con que algoritmo se tomo: si cambia el lector, hay que rehacerla. */
        private String algoritmo;
    }
}
