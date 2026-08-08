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

    /** Mismo criterio, para el titular de una invitacion. */
    boolean estaAdentroInvitado(Long invitacionId);

    /**
     * Mismo criterio, para un vehiculo: su ultimo paso permitido fue una
     * ENTRADA, asi que el carro esta en el conjunto.
     *
     * <p>Se mira el evento del VEHICULO y no el de su dueno: en una casa el
     * carro lo saca quien lo necesite ese dia, y son cosas distintas — la
     * senora puede estar adentro con el carro afuera.</p>
     */
    boolean estaAdentroVehiculo(Long vehiculoId);

    PresenciaResponse conteo(Long conjuntoId);

    /**
     * El mismo conteo dejando a una persona fuera de la poblacion.
     *
     * <p>Lo usa el tablero del administrador, que no se cuenta a si mismo. La
     * porteria sigue usando el conteo completo: ahi el numero responde "¿a
     * quien evacuo?" y nadie sobra.</p>
     */
    PresenciaResponse conteo(Long conjuntoId, Long exceptoPersonaId);
}
