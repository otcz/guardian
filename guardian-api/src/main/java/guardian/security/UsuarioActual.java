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

    /**
     * Sede del llamante, exigida.
     *
     * <p>Falla con 403 en vez de devolver null porque un {@code conjuntoId}
     * nulo llegando a los repositorios produce {@code WHERE conjunto_id =
     * NULL}, que en SQL nunca es cierto: el back-office entero se veria vacio
     * en lugar de dar un mensaje, y eso se diagnostica mal durante horas.</p>
     */
    public Long conjuntoId() {
        Long sede = obtener().getConjuntoId();
        if (sede == null) {
            throw GuardianException.sinPermiso(MensajesGlobales.SIN_SEDE_ELEGIDA);
        }
        return sede;
    }

    /** Null cuando el super administrador aun no ha entrado a ninguna sede. */
    public Long conjuntoIdOpcional() {
        return obtener().getConjuntoId();
    }

    public boolean esSuperAdmin() {
        return guardian.constant.Codigos.ROL_SUPER_ADMIN.equals(obtener().getRol());
    }

    public Long personaId() {
        return obtener().getPersonaId();
    }

    /** Etiqueta para los campos de auditoria de BaseEntity. */
    public String documento() {
        return obtener().getDocumento();
    }
}
