package guardian.dto.acceso;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Identificar a alguien por su documento en vez de por su QR.
 *
 * <p>Es el camino que comparten los tres lectores de la porteria: el de codigo
 * de barras —que del PDF417 de la cedula saca el numero, no un payload de
 * GUARDIAN—, el guardia tecleando la cedula, y el lector de huella cuando lo
 * haya.</p>
 *
 * <p><b>Un documento NO es un secreto y un QR firmado si.</b> Por este camino
 * el control real es el que ya manda en la garita: la foto grande y el guardia
 * comparando la cara. Por eso la ficha que vuelve es identica — lo que cambia
 * es como se llego a ella, y eso queda escrito en la bitacora.</p>
 */
@Data
public class VerificarDocumentoRequest {

    @NotBlank(message = "Escribe el numero de documento")
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String documento;

    /** Porteria por donde se esta atendiendo. Opcional si el conjunto tiene una sola. */
    private Long puntoAccesoId;
}
