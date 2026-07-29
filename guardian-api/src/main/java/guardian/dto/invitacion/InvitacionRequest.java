package guardian.dto.invitacion;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Creacion de una invitacion por el residente. La casa nunca viene en el
 * request: siempre es la del anfitrion en sesion.
 */
@Data
public class InvitacionRequest {

    @NotBlank(message = "Escribe el nombre del invitado")
    @Size(max = 150)
    private String nombreInvitado;

    @NotBlank(message = "Escribe el documento del invitado")
    @Size(max = 20)
    private String documentoInvitado;

    /** Null = a pie. */
    @Size(max = 10)
    private String placa;

    /** Null = desde ahora. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date vigenciaDesde;

    /** Null = hasta el final del dia de la vigencia inicial. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date vigenciaHasta;

    /** Null = 1 (un solo ingreso). */
    @Min(value = 1, message = "Minimo un ingreso")
    @Max(value = 20, message = "Maximo 20 ingresos")
    private Integer usosMaximos;
}
