package guardian.dto.sede;

import lombok.Builder;
import lombok.Data;

/** Una sede vista desde el panel del super administrador. */
@Data
@Builder
public class SedeResponse {

    private Long id;
    private String nombre;
    private String direccion;
    private String activo;
    private String bloqueado;

    /** Contadores para decidir a que sede entrar sin tener que entrar. */
    private long casas;
    private long personas;
    private long usuarios;

    /** false cuando la sede todavia no tiene a quien administrarla. */
    private boolean tieneAdministrador;
}
