package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class PersonaRequest {

    /** Codigo del grupo TIPO_DOCUMENTO (CC, TI, CE, PA, RC). Null = CC. */
    @Size(max = 5)
    private String tipoDocumento;

    @NotBlank(message = "Escribe el numero de documento")
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String documento;

    @NotBlank(message = "Escribe los nombres")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Escribe los apellidos")
    @Size(max = 100)
    private String apellidos;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaNacimiento;

    private String fotoUrl;

    @Size(max = 30)
    private String telefono;

    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120)
    private String email;

    /** Casa donde vive. Null para guardias o administradores que no residen en el conjunto. */
    private Long casaId;

    /** Codigo del grupo PARENTESCO. Obligatorio cuando se envia casaId. */
    private String parentesco;

    /**
     * Codigo del grupo ROL. Si viene, el alta tambien crea la cuenta de acceso
     * (habilitada, clave inicial 0000 y cambio forzado al entrar).
     * Null = persona sin cuenta, p. ej. un menor que solo necesita QR.
     */
    private String rolUsuario;
}
