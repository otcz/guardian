package guardian.util;

import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;

/**
 * Frontera entre sedes. Helper puro, sin estado.
 *
 * <p>Lanza <b>404 y no 403</b> a proposito: un administrador que tantea ids
 * ajenos no debe poder distinguir "existe en otra sede" de "no existe". Con un
 * 403 podria mapear el contenido de las demas sedes contando respuestas.</p>
 *
 * <p>Falla cerrado: si el llamante no tiene sede, no pasa.</p>
 */
public final class TenantGuard {

    private TenantGuard() {
    }

    public static void exigirMismaSede(Long sedeDeLaEntidad, Long sedeDelToken) {
        if (sedeDeLaEntidad == null || sedeDelToken == null
                || !sedeDeLaEntidad.equals(sedeDelToken)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
    }

    public static boolean esDeLaSede(Long sedeDeLaEntidad, Long sedeDelToken) {
        return sedeDeLaEntidad != null && sedeDelToken != null
                && sedeDeLaEntidad.equals(sedeDelToken);
    }
}
