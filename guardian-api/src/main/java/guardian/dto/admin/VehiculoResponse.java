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
}
