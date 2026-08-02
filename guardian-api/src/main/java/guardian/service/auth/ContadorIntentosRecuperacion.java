package guardian.service.auth;

import guardian.repository.GdCodigoRecuperacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuenta los intentos fallidos contra un codigo, en su PROPIA transaccion.
 *
 * <p><b>Por que existe este bean.</b> El intento fallido termina lanzando una
 * excepcion, y una excepcion tumba la transaccion — incluida la escritura del
 * contador. El resultado era que {@code intentos} se quedaba en cero para
 * siempre y el tope de cinco no frenaba nada: seis digitos con intentos
 * ilimitados se adivinan en minutos.</p>
 *
 * <p>Va en un bean aparte y no en un metodo del propio service porque una
 * llamada a {@code this.metodo()} no pasa por el proxy de Spring, y la
 * anotacion de propagacion no tendria ningun efecto — el mismo bug, pero mas
 * dificil de ver.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContadorIntentosRecuperacion {

    private final GdCodigoRecuperacionRepository codigoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fallo(Long codigoId) {
        codigoRepository.sumarIntento(codigoId);
        log.warn("[recuperacion] intento fallido codigoId={}", codigoId);
    }
}
