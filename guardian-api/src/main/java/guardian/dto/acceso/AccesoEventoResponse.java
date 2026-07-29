package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Un renglon del historico de accesos. Reemplaza la minuta de papel.
 */
@Data
@Builder
public class AccesoEventoResponse {

    private Long id;
    private Date fechaEvento;
    private String sentido;
    private String modo;
    private String resultado;
    private String motivoDenegacion;

    private String personaNombre;
    private String personaDocumento;
    private String casaIdentificador;
    private String vehiculoPlaca;
    private String guardiaNombre;
    private String puntoAcceso;

    /** Marca los cruces de INVITADOS para que la bitacora los distinga. */
    private boolean invitado;
}
