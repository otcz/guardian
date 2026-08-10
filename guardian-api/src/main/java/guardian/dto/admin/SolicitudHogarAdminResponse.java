package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Una solicitud de union al hogar en la bandeja del administrador. */
@Data
@Builder
public class SolicitudHogarAdminResponse {

    private Long id;
    private String nombreCompleto;
    private String documento;
    private String casaIdentificador;
    private String parentesco;
    private String titularNombre;
    private String estado;
    private Date fechaSolicitud;
}
