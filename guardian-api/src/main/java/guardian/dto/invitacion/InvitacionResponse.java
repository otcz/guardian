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
    private Integer usosMaximos;
    private Integer usosRealizados;

    /** VIGENTE | NO_VIGENTE | AGOTADA | VENCIDA | REVOCADA — derivado, no almacenado. */
    private String estado;

    private String casaIdentificador;
    private String anfitrionNombre;

    /** Identificador del link publico que se comparte con el invitado. */
    private String codigoPublico;

    /** Contenido del QR, por si el anfitrion lo muestra directo desde su pantalla. */
    private String payload;
}
