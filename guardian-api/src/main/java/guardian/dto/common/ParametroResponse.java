package guardian.dto.common;

import lombok.Builder;
import lombok.Data;

/**
 * Una opcion del catalogo. Es lo que llena los desplegables del frontend.
 */
@Data
@Builder
public class ParametroResponse {

    private Long id;
    private String grupo;
    private String codigo;
    private String valor;
    private Integer orden;

    /** El sistema la referencia por codigo: se renombra, no se desactiva. */
    private boolean protegido;

    /**
     * Siempre true en los listados que alimentan los desplegables —ahi solo
     * viajan las activas—. Solo distingue algo en el panel de Configuracion.
     */
    private boolean activo;
}
