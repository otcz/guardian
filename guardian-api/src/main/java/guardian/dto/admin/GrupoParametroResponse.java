package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

/** Un grupo del catalogo tal como lo lista la pantalla de Configuracion. */
@Data
@Builder
public class GrupoParametroResponse {

    private String grupo;
    /** Nombre legible: el administrador no tiene por que leer MOTIVO_DENEGACION. */
    private String nombre;
    private String descripcion;

    private long opciones;

    /**
     * false en los grupos estructurales. Ahi se puede renombrar una opcion
     * pero no agregar ni quitar: el codigo ramifica sobre esos codigos.
     */
    private boolean ampliable;
}
