package guardian.dto.residente;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.util.Date;

/** Los datos con los que alguien se registra dentro de un hogar existente. */
@Data
public class RegistroHogarRequest {

    @Size(max = 5)
    private String tipoDocumento;

    @NotBlank(message = "Escribe tu numero de documento")
    @Size(max = 20)
    private String documento;

    @NotBlank(message = "Escribe tus nombres")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Escribe tus apellidos")
    @Size(max = 100)
    private String apellidos;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaNacimiento;

    @Size(max = 30)
    private String telefono;

    /**
     * Obligatorio aca y no en el alta que hace el administrador: quien se
     * registra por este camino SIEMPRE recibe cuenta, y sin correo esa cuenta
     * nace sin forma de recuperar la clave.
     */
    @NotBlank(message = "Escribe tu correo")
    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120)
    private String email;

    /** Codigo del grupo PARENTESCO. TITULAR no: esa casa ya tiene uno. */
    @NotBlank(message = "Elige tu parentesco")
    private String parentesco;
}
