package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

/**
 * Una persona ofrecida al guardia cuando busca por nombre.
 *
 * <p>Lo mínimo para elegir sin equivocarse: la foto y la casa. En un conjunto
 * hay varios "Carlos Pérez", y el documento solo desempata si el guardia lo
 * tiene a la vista — que es justo lo que no pasa cuando alguien llegó sin
 * cédula, que es el caso que trae aquí al guardia.</p>
 *
 * <p>NO trae veredicto ni payload: esto no autoriza nada. Elegir un candidato
 * dispara la verificación normal por documento, que es la que decide.</p>
 */
@Data
@Builder
public class CandidatoGaritaResponse {

    private Long personaId;
    private String nombreCompleto;
    private String documento;
    private String casaIdentificador;
    private String fotoUrl;
}
