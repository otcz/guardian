package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Estado de la solicitud de vehiculo, tal como la ve la casa que la hizo. */
@Data
@Builder
public class SolicitudVehiculoResponse {

    private Long id;
    private String placa;

    private String tipo;
    private String marca;
    private String color;

    /** Etiquetas del catalogo: la pantalla muestra "Mazda", no "MAZDA_01". */
    private String tipoNombre;
    private String marcaNombre;
    private String colorNombre;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    private String estado;

    /** Escrito para el residente, no para el registro tecnico. */
    private String motivoRechazo;

    private Date fechaSolicitud;
}
