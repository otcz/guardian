package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

/**
 * La credencial del residente tal como la ve en su telefono.
 */
@Data
@Builder
public class MiQrResponse {

    /** Contenido que la app dibuja como QR. Null cuando todavia no hay foto. */
    private String payload;

    /**
     * true cuando falta la foto y por eso no hay credencial.
     *
     * <p>Es un ESTADO, no un error: al residente se le ofrece subirla y con
     * eso queda resuelto. Antes esto llegaba como un 400 y la pantalla no
     * podia hacer mas que pintar un aviso rojo sin salida.</p>
     */
    private boolean necesitaFoto;

    private String nombreCompleto;
    private String tipoDocumento;
    private String documento;
    private String casaIdentificador;
    private String fotoUrl;
}
