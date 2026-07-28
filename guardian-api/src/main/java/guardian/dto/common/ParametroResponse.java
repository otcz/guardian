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
    private boolean protegido;
}
