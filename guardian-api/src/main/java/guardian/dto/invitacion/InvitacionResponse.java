package guardian.dto.invitacion;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Una invitacion tal como la ven el anfitrion y el administrador.
 */
@Data
@Builder
public class InvitacionResponse {

    private Long id;
    private String nombreInvitado;
    private String documentoInvitado;
    private String placa;
    private Date vigenciaDesde;
    private Date vigenciaHasta;

    /** VIGENTE | NO_VIGENTE | AGOTADA | VENCIDA | REVOCADA — derivado, no almacenado. */
    private String estado;

    private String casaIdentificador;
    private String anfitrionNombre;

    /**
     * Si su ultimo paso por la porteria fue una ENTRADA: el invitado sigue en
     * el conjunto. Es lo que responde "¿ya se fue?" sin llamar a la porteria.
     *
     * <p>Es lo que dice la bitacora, no una ubicacion: quien salio caminando
     * sin escanear figura adentro hasta que vuelva a pasar.</p>
     */
    private boolean adentro;

    /** Identificador del link publico que se comparte con el invitado. */
    private String codigoPublico;

    /** Contenido del QR, por si el anfitrion lo muestra directo desde su pantalla. */
    private String payload;
}
