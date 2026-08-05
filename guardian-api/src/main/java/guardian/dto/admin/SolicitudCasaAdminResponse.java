package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Una solicitud en la bandeja del administrador, con quien la pide. */
@Data
@Builder
public class SolicitudCasaAdminResponse {

    private Long id;
    private Long personaId;
    private String nombreCompleto;
    private String documento;
    private String casaIdentificador;
    private String parentesco;
    private String estado;
    private Date fechaSolicitud;

    /**
     * Si la casa ya tiene titular. El administrador decide distinto segun el
     * caso: aprobar a alguien como TITULAR de una casa que ya lo tiene se
     * rechaza, y aprobar al primero de una casa vacia lo vuelve el titular.
     */
    private boolean casaConTitular;
}
