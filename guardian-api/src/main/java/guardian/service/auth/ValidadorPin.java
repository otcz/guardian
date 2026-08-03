package guardian.service.auth;

import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import guardian.util.PinUtil;

/**
 * La puerta unica por la que pasa TODO PIN elegido por una persona.
 *
 * <p>Son tres caminos —el cambio propio, el que asigna el administrador y la
 * recuperacion por correo— y los tres tienen que exigir lo mismo. Con la regla
 * repetida en cada uno, el mas flojo se vuelve la puerta de atras: bastaria con
 * recuperar la clave para ponerse el 1234 que el cambio normal rechaza.</p>
 */
public final class ValidadorPin {

    private ValidadorPin() {
    }

    /**
     * @param documento el de la persona, para impedir que el PIN salga de ahi.
     *                  Puede ser null cuando no se conoce.
     * @throws GuardianException 400 con el motivo concreto. Se distinguen los
     *         tres porque quien esta eligiendo su PIN necesita saber QUE
     *         corregir; no es el caso de un atacante adivinando.
     */
    public static void exigirValido(String pin, String documento) {
        if (!PinUtil.tieneFormaDePin(pin)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.PIN_FORMA_INVALIDA);
        }
        if (PinUtil.esTrivial(pin)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.PIN_TRIVIAL);
        }
        if (PinUtil.saleDelDocumento(pin, documento)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.PIN_SALE_DEL_DOCUMENTO);
        }
    }
}
