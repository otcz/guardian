package guardian.dto.admin;

import guardian.constant.Codigos;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Clave que la administracion le asigna a una cuenta.
 *
 * <p>Mismos limites que el cambio de clave propio: si el panel aceptara una
 * clave mas debil que la que el sistema le exige al dueno, la puerta ancha
 * seria justamente la que abre el administrador.</p>
 */
@Data
public class ClaveAsignadaRequest {

    // Tope 72: BCrypt ignora en silencio todo byte despues del 72. Aceptar una
    // clave mas larga le prometeria al usuario una seguridad que no tiene.
    @NotBlank(message = "Escribe la nueva contrasena")
    @Size(min = Codigos.CLAVE_LONGITUD_MINIMA, max = Codigos.CLAVE_LONGITUD_MAXIMA,
            message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
