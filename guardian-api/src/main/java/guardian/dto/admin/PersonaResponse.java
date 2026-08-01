package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class PersonaResponse {

    private Long id;
    private String tipoDocumento;
    private String documento;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private Date fechaNacimiento;
    private Integer edad;
    private String fotoUrl;
    private String telefono;
    private String email;
    private String activo;

    private Long casaId;
    private String casaIdentificador;
    private String parentesco;

    /** Si ya tiene credencial QR activa. */
    private boolean tieneCredencial;

    /** Rol del usuario, o null si la persona no tiene cuenta en la aplicacion. */
    private String rol;
    private String usuarioActivo;
}
