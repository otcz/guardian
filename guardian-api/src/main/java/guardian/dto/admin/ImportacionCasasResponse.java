package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resultado de una carga masiva, fila por fila.
 *
 * <p>Nunca un simple "listo": un archivo de doscientas casas donde tres quedaron
 * fuera y no se dice cuales obliga al administrador a revisar la lista entera a
 * mano — que es justo lo que la carga masiva venia a evitar.</p>
 */
@Data
@Builder
public class ImportacionCasasResponse {

    private int leidas;
    private int creadas;
    private int repetidas;
    private int conError;

    /** Solo las filas que NO se crearon. Las buenas no hay que revisarlas. */
    private List<FilaRechazada> rechazos;

    @Data
    @Builder
    public static class FilaRechazada {
        /** Numero de fila del Excel, contando el encabezado: el que se ve al abrirlo. */
        private int fila;
        private String tipo;
        private String numero;
        private String motivo;
    }
}
