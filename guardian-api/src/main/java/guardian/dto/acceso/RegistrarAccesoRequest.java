package guardian.dto.acceso;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegistrarAccesoRequest {

    /**
     * El mismo payload que se verifico. Se reenvia y se vuelve a validar la
     * firma en vez de confiar en un id: asi no existe forma de registrar un
     * ingreso sin haber escaneado realmente el codigo.
     */
    @NotBlank(message = "Falta el codigo escaneado")
    private String payload;

    /** PEATON o VEHICULO. */
    @NotBlank(message = "Indica si entra a pie o en vehiculo")
    private String modo;

    /** Obligatorio cuando el modo es VEHICULO. */
    private Long vehiculoId;

    /** Null para aceptar el sentido que sugiere el sistema. */
    private String sentido;

    private Long puntoAccesoId;
}
