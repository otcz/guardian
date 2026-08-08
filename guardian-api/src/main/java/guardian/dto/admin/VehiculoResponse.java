package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehiculoResponse {

    private Long id;
    private String placa;
    /** Codigos del catalogo: son lo que viaja de vuelta al editar. */
    private String tipo;
    private String marca;
    private String color;

    /** Los mismos, ya traducidos. La tabla muestra "Volkswagen", no VOLKSWAGEN. */
    private String tipoNombre;
    private String marcaNombre;
    private String colorNombre;

    private String activo;

    /** Llave del administrador: gana sobre "activo". */
    private String bloqueado;
    private String motivoBloqueo;

    private Long casaId;
    private String casaIdentificador;

    /**
     * Si el carro esta en el conjunto: su ultimo paso permitido fue una
     * ENTRADA.
     *
     * <p>Solo se calcula donde la lista es corta —los vehiculos de UNA casa—
     * porque cuesta una consulta por fila. En el listado del administrador,
     * que puede traer cientos, se queda en false; ahi la pregunta la responde
     * la bitacora, que para eso pagina y filtra.</p>
     */
    private boolean adentro;
}
