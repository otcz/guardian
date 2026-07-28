package guardian.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Envelope estable de error. Que la forma no cambie entre un 400 y un 500
 * permite que el interceptor del frontend tenga un solo camino para mostrar el
 * mensaje al usuario.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {

    private Date momento;
    private int estado;
    private String mensaje;
    /** Errores por campo cuando falla la validacion de un DTO. Null en el resto. */
    private List<String> detalles;
}
