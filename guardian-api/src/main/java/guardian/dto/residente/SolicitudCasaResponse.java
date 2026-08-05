package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Estado de la solicitud, tal como la ve el residente que la hizo. */
@Data
@Builder
public class SolicitudCasaResponse {

    private Long id;
    private Long casaId;
    private String casaIdentificador;
    private String parentesco;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    private String estado;

    /** Escrito para el residente, no para el registro tecnico. */
    private String motivoRechazo;

    private Date fechaSolicitud;
}
