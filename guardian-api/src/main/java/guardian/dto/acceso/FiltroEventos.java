package guardian.dto.acceso;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * Lo que la pantalla de bitacora le pide al servidor.
 *
 * <p>Un objeto y no ocho parametros sueltos: agregar un filtro mas no puede
 * significar tocar la firma del controller, la de la interface y la del
 * service. Spring lo arma solo desde el query string.</p>
 *
 * <p>Las listas son multiseleccion —un desplegable donde se marcan varios— y
 * vacia significa "todos": en un autofiltro, no marcar nada NO es marcar
 * nada.</p>
 */
@Data
public class FiltroEventos {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date hasta;

    private Long casaId;

    /** PERMITIDO / DENEGADO. */
    private List<String> resultados;

    /** E / S. */
    private List<String> sentidos;

    /** PEATON / VEHICULO. */
    private List<String> modos;

    /** Codigos del catalogo MOTIVO_DENEGACION. */
    private List<String> motivos;

    /** Porterias por las que paso, por id. */
    private List<Long> porteriaIds;

    /** Busqueda libre sobre nombre, documento, casa y placa del evento. */
    private String texto;
}
