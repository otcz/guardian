package guardian.service.acceso;

import guardian.dto.acceso.PresenciaResponse;

public interface PresenciaService {

    /**
     * Una persona esta adentro cuando su ultimo evento permitido fue una
     * ENTRADA. Sin ventana de tiempo: quien entro ayer y nunca registro salida
     * sigue adentro hasta que salga, y eso es exactamente lo que la porteria
     * necesita saber.
     */
    boolean estaAdentro(Long personaId);

    PresenciaResponse conteo(Long conjuntoId);
}
