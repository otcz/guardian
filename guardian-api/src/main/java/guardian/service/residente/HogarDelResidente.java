package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdResidenteCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Las dos preguntas que se hace TODA operacion de autogestion antes de tocar
 * nada: en que casa vive quien esta pidiendo, y si es el titular de ella.
 *
 * <p>Estaba duplicada en cada service del residente. Duplicada, el dia que
 * cambie la regla de quien manda en un hogar hay que acordarse de todos los
 * sitios donde se copio — y el que se olvide queda abierto.</p>
 */
@Component
@RequiredArgsConstructor
public class HogarDelResidente {

    private final GdResidenteCasaRepository residenteCasaRepository;

    /** La casa del solicitante. Sin casa no hay autogestion posible. */
    public GdCasa casa(UsuarioAutenticado usuario) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(usuario.getPersonaId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .orElseThrow(() -> GuardianException.solicitudInvalida(MensajesGlobales.SIN_CASA));
    }

    public boolean esTitular(UsuarioAutenticado usuario, GdCasa casa) {
        return residenteCasaRepository
                .findByPersonaIdAndCasaId(usuario.getPersonaId(), casa.getId())
                .map(vinculo -> Codigos.PARENTESCO_TITULAR.equals(vinculo.getParentesco()))
                .orElse(false);
    }

    /**
     * Igual que {@link #esTitular}, pero corta. El mensaje lo pone quien llama:
     * "no puedes agregar familiares" y "no puedes registrar vehiculos" son dos
     * frustraciones distintas y merecen dos explicaciones distintas.
     */
    public void exigirTitular(UsuarioAutenticado usuario, GdCasa casa, String mensaje) {
        if (!esTitular(usuario, casa)) {
            throw GuardianException.sinPermiso(mensaje);
        }
    }
}
