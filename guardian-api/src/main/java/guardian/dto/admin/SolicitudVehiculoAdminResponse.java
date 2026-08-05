package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Una solicitud de vehiculo en la bandeja del administrador. */
@Data
@Builder
public class SolicitudVehiculoAdminResponse {

    private Long id;
    private String placa;

    /** Etiquetas del catalogo: la bandeja muestra "Camioneta", no "CAM". */
    private String tipoNombre;
    private String marcaNombre;
    private String colorNombre;

    private Long casaId;
    private String casaIdentificador;

    /** Quien lo pidio: el titular de esa casa. */
    private String solicitante;
    private String documento;

    private String estado;
    private Date fechaSolicitud;
}
