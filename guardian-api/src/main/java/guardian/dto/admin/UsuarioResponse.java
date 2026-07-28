package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class UsuarioResponse {

    private Long id;
    private Long personaId;
    private String documento;
    private String nombreCompleto;
    private String rol;
    private String activo;
    private boolean requiereCambioClave;
    private Date fechaUltimoIngreso;
}
