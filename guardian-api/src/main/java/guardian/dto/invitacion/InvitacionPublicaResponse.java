package guardian.dto.invitacion;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Lo que ve el INVITADO al abrir el link compartido, sin iniciar sesion.
 * Solo lo necesario para presentarse en porteria — nada del conjunto que no
 * le corresponda saber.
 */
@Data
@Builder
public class InvitacionPublicaResponse {

    private String nombreInvitado;
    private String casaIdentificador;
    private String anfitrionNombre;
    private Date vigenciaDesde;
    private Date vigenciaHasta;
    private Integer usosRestantes;
    private String estado;

    /** Contenido del QR que la pagina publica dibuja. */
    private String payload;
}
