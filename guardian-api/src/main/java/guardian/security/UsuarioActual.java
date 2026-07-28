package guardian.security;

import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acceso a la identidad del llamante desde los services.
 *
 * <p>Es un bean y no un metodo estatico para que los tests puedan sustituirlo
 * sin tener que armar un SecurityContext completo.</p>
 */
@Component
public class UsuarioActual {

    /**
     * @throws GuardianException 401 si no hay sesion. Que falle fuerte es
     *         intencional: un service que necesita saber quien actua no puede
     *         seguir con un null y decidir despues.
     */
    public UsuarioAutenticado obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado)) {
            throw GuardianException.noAutorizado(MensajesGlobales.SESION_REQUERIDA);
        }
        return (UsuarioAutenticado) auth.getPrincipal();
    }

    public Long conjuntoId() {
        return obtener().getConjuntoId();
    }

    public Long personaId() {
        return obtener().getPersonaId();
    }

    /** Etiqueta para los campos de auditoria de BaseEntity. */
    public String documento() {
        return obtener().getDocumento();
    }
}
