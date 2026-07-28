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
}
