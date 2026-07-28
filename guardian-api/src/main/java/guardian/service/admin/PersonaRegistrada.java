package guardian.service.admin;

import guardian.dto.admin.PersonaResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de dar de alta una persona.
 *
 * <p>Lleva el payload del QR ademas de la persona porque el administrador que
 * acaba de registrar a alguien casi siempre quiere imprimirle el carnet en ese
 * momento. Obligarlo a una segunda llamada convertiria el alta en dos pasos que
 * nadie recuerda completar.</p>
 */
@Getter
@AllArgsConstructor
public class PersonaRegistrada {

    private final PersonaResponse persona;

    /** Null cuando la persona todavia no tiene foto y no se le pudo emitir credencial. */
    private final String payloadQr;
}
