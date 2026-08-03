package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CasaResponse {

    private Long id;
    private String identificador;
    private String torre;
    private String numero;
    private String activo;

    /** Llave del administrador: gana sobre "activo". */
    private String bloqueado;
    private String motivoBloqueo;

    /** Cuantas personas viven ahi. Evita que el administrador tenga que abrir la casa para saberlo. */
    private long residentes;
    private long vehiculos;
}
