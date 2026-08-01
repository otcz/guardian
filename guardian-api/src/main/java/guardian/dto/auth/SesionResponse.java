package guardian.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Identidad del usuario en sesion. Es lo que el frontend usa para pintar el
 * encabezado y decidir que menu mostrar.
 */
@Data
@Builder
public class SesionResponse {

    private Long usuarioId;
    private Long personaId;
    private String documento;
    private String nombreCompleto;
    private String rol;
    private String fotoUrl;
    private String casaIdentificador;

    /**
     * Sede en la que se esta operando. Para el super administrador es
     * imprescindible verlo en pantalla: sin el nombre a la vista, editar la
     * casa equivocada de la sede equivocada deja de ser improbable.
     */
    private Long sedeId;
    private String sedeNombre;

    /** true cuando un super administrador esta operando dentro de una sede. */
    private boolean sedeSuplantada;
}
