package guardian.dto.invitacion;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
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
    @Pattern(regexp = "[A-Za-z0-9.-]{3,20}", message = "El documento no parece valido")
    private String documentoInvitado;

    /** Null = a pie. Acepta espacios y guiones; el service la normaliza. */
    @Pattern(regexp = "[A-Za-z0-9\\s-]{3,10}", message = "La placa no parece valida")
    private String placa;

    /**
     * Null = desde ahora. Sin patron fijo: el cliente manda el instante CON su
     * offset (ej. -05:00). Fijar un patron sin zona hacia que Jackson lo
     * leyera como UTC y la invitacion muriera horas antes de la medianoche
     * local del residente.
     */
    private Date vigenciaDesde;

    /** Null = hasta el final del dia de la vigencia inicial. */
    private Date vigenciaHasta;

    /** Null = 1 (un solo ingreso). */
    @Min(value = 1, message = "Minimo un ingreso")
    @Max(value = 20, message = "Maximo 20 ingresos")
    private Integer usosMaximos;
}
