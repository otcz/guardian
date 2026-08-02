package guardian.dto.residente;

import guardian.dto.common.ParametroResponse;
import lombok.Builder;

import java.util.List;
import lombok.Data;

/**
 * Lo que ve quien abre el enlace ANTES de registrarse.
 *
 * <p>Solo el conjunto y la casa: lo justo para que sepa a donde se esta
 * uniendo y no siga si el codigo llego por error. Nombres, telefonos y
 * documentos de la familia NO viajan a una pantalla sin sesion.</p>
 */
@Data
@Builder
public class HogarPublicoResponse {

    private String conjuntoNombre;
    private String casaIdentificador;
    private String titularNombre;
    private boolean vigente;

    /**
     * Las opciones del formulario viajan CON la respuesta.
     *
     * <p>Esta pantalla no tiene sesion, asi que no puede pedir /api/parametros.
     * Mandar solo lo que este formulario necesita es mas barato que abrir el
     * catalogo entero al publico.</p>
     */
    private List<ParametroResponse> parentescos;
    private List<ParametroResponse> tiposDocumento;
}
