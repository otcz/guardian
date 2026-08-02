package guardian.security;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;

/**
 * Quien puede hacer que, cuando el rol solo no alcanza como respuesta.
 *
 * <p>Vive aparte de {@link UsuarioActual} porque aquel resuelve la identidad
 * desde el contexto de seguridad y este decide sobre una identidad que ya
 * recibio. Los services reciben el {@code UsuarioAutenticado} por parametro, y
 * asi la regla se prueba sin montar un SecurityContext.</p>
 */
public final class Autoridad {

    private Autoridad() {
    }

    /**
     * Exige que el llamante sea el operador de la plataforma.
     *
     * <p>Reservado para el borrado fisico. Todo lo demas del back-office lo
     * hace el administrador de la sede: desactivar, bloquear, revocar. Borrar
     * es distinto porque deja un hueco en la auditoria — la fila que explicaba
     * un ingreso, una cuenta o un vehiculo desaparece— y quien la borra suele
     * ser parte de la disputa que ese registro resolveria.</p>
     *
     * <p>Sirve tambien cuando el super administrador esta operando dentro de
     * una sede: entrar a una sede conserva su rol en el token y solo marca la
     * sesion como suplantada, asi que la comprobacion no cambia.</p>
     *
     * @throws GuardianException 403 si no lo es.
     */
    public static void exigirSuperAdmin(UsuarioAutenticado ejecutor) {
        if (ejecutor == null || !Codigos.ROL_SUPER_ADMIN.equals(ejecutor.getRol())) {
            throw GuardianException.sinPermiso(MensajesGlobales.BORRADO_SOLO_SUPER_ADMIN);
        }
    }
}
