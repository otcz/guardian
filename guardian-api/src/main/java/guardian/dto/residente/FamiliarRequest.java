package guardian.dto.residente;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Email;
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

    /** Codigo del grupo TIPO_DOCUMENTO (CC, TI, CE, PA, RC). Null = CC. */
    @Size(max = 5)
    private String tipoDocumento;

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

    /**
     * Con correo, el familiar recibe cuenta de RESIDENTE y entra a la
     * aplicacion con su documento y el PIN inicial. Sin correo queda registrado
     * para la porteria pero sin cuenta — el caso del nino que todavia no tiene
     * telefono ni correo propio.
     *
     * <p>El rol NO viaja en el request: lo fija el service. Si viniera de
     * afuera, un titular podria crearse un ADMIN desde el celular.</p>
     */
    @Email(message = "El correo no parece valido")
    @Size(max = 120)
    private String email;

    /** Codigo del grupo PARENTESCO: ESPOSO, ESPOSA, HIJO, INVITADO, OTRO. */
    @NotBlank(message = "Selecciona el parentesco")
    private String parentesco;
}
