package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

/**
 * La credencial del residente tal como la ve en su telefono.
 */
@Data
@Builder
public class MiQrResponse {

    /** Contenido que la app dibuja como QR. */
    private String payload;

    private String nombreCompleto;
    private String tipoDocumento;
    private String documento;
    private String casaIdentificador;
    private String fotoUrl;
}
