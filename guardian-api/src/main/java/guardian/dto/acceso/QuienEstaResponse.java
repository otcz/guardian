package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Alguien que esta adentro o afuera del conjunto ahora mismo.
 *
 * <p>Sale del ULTIMO evento permitido de cada persona, que es de donde el
 * sistema deduce la presencia. Trae el nombre y la casa copiados en ese evento
 * —no consultados de nuevo— porque asi la lista responde por lo que de verdad
 * quedo registrado en la porteria.</p>
 */
@Data
@Builder
public class QuienEstaResponse {

    private Long personaId;
    private String nombreCompleto;
    private String casaIdentificador;

    /** Cuando fue ese ultimo paso. Responde "desde cuando esta adentro". */
    private Date desde;

    /** Entro en carro: la placa con la que quedo registrado. */
    private String vehiculoPlaca;

    private boolean invitado;
}
