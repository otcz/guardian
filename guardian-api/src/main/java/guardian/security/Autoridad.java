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

    /**
     * Nadie crea un par suyo ni nada por encima.
     *
     * <p>Antes solo se blindaba SUPER_ADMIN, y un administrador de sede podia
     * nombrar a otro administrador de su sede. Eso convierte una cuenta
     * comprometida en varias: quien entra crea un segundo administrador, y
     * quitarle el acceso al primero ya no cierra nada.</p>
     *
     * <p>Queda entonces:</p>
     * <ul>
     *   <li>SUPER_ADMIN puede nombrar ADMIN, GUARDIA y RESIDENTE.</li>
     *   <li>ADMIN puede nombrar GUARDIA y RESIDENTE.</li>
     * </ul>
     *
     * <p>SUPER_ADMIN no lo asigna nadie, ni el mismo: se siembra en el
     * arranque y punto. No basta con no ponerlo en el catalogo — el dia que
     * alguien lo agregue "para que aparezca en el combo", esta linea es lo
     * unico que impide que cualquier administrador se ascienda con un PATCH.</p>
     *
     * @throws GuardianException 403 si el rol pedido esta al nivel del
     *         ejecutor o por encima.
     */
    public static void exigirRolAsignablePor(UsuarioAutenticado ejecutor, String rol) {
        if (Codigos.ROL_SUPER_ADMIN.equals(rol)) {
            throw GuardianException.sinPermiso(MensajesGlobales.ROL_NO_ASIGNABLE);
        }
        if (Codigos.ROL_ADMIN.equals(rol)
                && (ejecutor == null || !Codigos.ROL_SUPER_ADMIN.equals(ejecutor.getRol()))) {
            throw GuardianException.sinPermiso(MensajesGlobales.ADMIN_SOLO_SUPER_ADMIN);
        }
    }

    /** Los roles que ESTE ejecutor puede asignar. Alimenta el combo. */
    public static java.util.List<String> rolesAsignablesPor(UsuarioAutenticado ejecutor) {
        if (ejecutor != null && Codigos.ROL_SUPER_ADMIN.equals(ejecutor.getRol())) {
            return java.util.Arrays.asList(
                    Codigos.ROL_ADMIN, Codigos.ROL_GUARDIA, Codigos.ROL_RESIDENTE);
        }
        return java.util.Arrays.asList(Codigos.ROL_GUARDIA, Codigos.ROL_RESIDENTE);
    }
}
