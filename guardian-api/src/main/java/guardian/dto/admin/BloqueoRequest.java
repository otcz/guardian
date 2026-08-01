package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Bloqueo administrativo. El motivo es obligatorio a proposito: un bloqueo sin
 * explicacion deja al siguiente administrador — y al guardia que tiene que dar
 * la cara en la porteria — sin saber por que esa persona no entra.
 */
@Data
public class BloqueoRequest {

    @NotBlank(message = "Escribe el motivo del bloqueo")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    private String motivo;
}
