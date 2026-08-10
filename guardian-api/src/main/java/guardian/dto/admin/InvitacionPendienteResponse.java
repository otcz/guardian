package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Una invitacion en la bandeja del administrador, esperando decision. */
@Data
@Builder
public class InvitacionPendienteResponse {

    private Long id;
    private String nombreInvitado;
    private String documentoInvitado;
    private String casaIdentificador;
    private String anfitrionNombre;
    private Date vigenciaDesde;
    private Date vigenciaHasta;
    private String estado;
    private Date fechaSolicitud;
}
