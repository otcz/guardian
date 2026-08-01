package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

/**
 * Miembro de la casa tal como lo ve el residente en "Mi hogar".
 */
@Data
@Builder
public class FamiliarResponse {

    private Long personaId;
    private String tipoDocumento;
    private String documento;
    private String nombreCompleto;
    private String parentesco;
    private String fotoUrl;
    private Integer edad;
    private String activo;
    private boolean tieneCredencial;

    /** Marca al propio solicitante para que la UI no le ofrezca inactivarse. */
    private boolean esUsuarioActual;
}
