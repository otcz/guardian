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
}
