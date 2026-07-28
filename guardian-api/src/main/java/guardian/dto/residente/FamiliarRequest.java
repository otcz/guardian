package guardian.dto.residente;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Alta de un miembro del nucleo familiar o un invitado, hecha por el propio
 * residente. La casa NO viene en el request: siempre es la del solicitante.
 */
@Data
public class FamiliarRequest {

    @NotBlank(message = "Escribe el numero de documento")
    @Size(max = 20)
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

    /** Codigo del grupo PARENTESCO: ESPOSO, ESPOSA, HIJO, INVITADO, OTRO. */
    @NotBlank(message = "Selecciona el parentesco")
    private String parentesco;
}
