package guardian.dto.acceso;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegistrarAccesoRequest {

    /**
     * El mismo payload que se verifico. Se reenvia y se vuelve a validar la
     * firma en vez de confiar en un id: asi no existe forma de registrar un
     * ingreso sin haber escaneado realmente el codigo.
     */
    @NotBlank(message = "Falta el codigo escaneado")
    private String payload;

    /** PEATON o VEHICULO. Validado aca para que a la bitacora no llegue basura. */
    @NotBlank(message = "Indica si entra a pie o en vehiculo")
    @Pattern(regexp = "PEATON|VEHICULO", message = "Modo desconocido")
    private String modo;

    /** Obligatorio cuando el modo es VEHICULO. */
    private Long vehiculoId;

    /** Null para aceptar el sentido que sugiere el sistema. */
    @Pattern(regexp = "[ES]", message = "Sentido desconocido")
    private String sentido;

    /**
     * true cuando el guardia decide conscientemente contradecir el sentido
     * inferido (CONTEXT.md seccion 4). Sin este flag, un sentido divergente se
     * rechaza: es una pantalla desactualizada, no una correccion.
     */
    private Boolean corregirSentido;

    private Long puntoAccesoId;
}
