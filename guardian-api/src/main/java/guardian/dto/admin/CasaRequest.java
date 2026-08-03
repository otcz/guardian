package guardian.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CasaRequest {

    /**
     * Codigo del grupo TIPO_VIVIENDA: CASA o APARTAMENTO.
     *
     * <p>Obligatorio. Antes era texto libre y opcional —"la torre"— y de ahi
     * salian identificadores incomparables: "Torre A", "TORRE A", "A" y vacio
     * convivian en el mismo conjunto. Toda unidad es una cosa o la otra.</p>
     *
     * <p>Se conserva el nombre del campo por compatibilidad con la columna
     * TORRE, que ddl-auto=update no renombra.</p>
     */
    @NotBlank(message = "Elige si es casa o apartamento")
    @Size(max = 20)
    private String torre;

    @NotBlank(message = "Escribe el numero de la casa")
    @Size(max = 20, message = "El numero no puede superar 20 caracteres")
    private String numero;

    private String observaciones;
}
