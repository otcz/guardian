package guardian.util;

/**
 * Normalizacion del correo, que en GUARDIAN no es un dato de contacto mas: es
 * la llave con la que alguien recupera su cuenta.
 *
 * <p>Minusculas y sin espacios SIEMPRE. La comparacion se hace contra lo
 * guardado, y quien escribe "Ana@Correo.com " al pedir su codigo no encontraria
 * la fila que dice "ana@correo.com". Guardado normalizado, las dos formas
 * llegan al mismo sitio.</p>
 */
public final class CorreoUtil {

    private CorreoUtil() {
    }

    /**
     * @return el correo en minusculas y recortado, o {@code null} si venia
     *         vacio. Cadena vacia y null significan lo mismo —no lo declaro— y
     *         guardar las dos dejaria dos formas de estar vacio que hay que
     *         preguntar por separado.
     */
    public static String normalizar(String email) {
        if (email == null) {
            return null;
        }
        String limpio = email.trim().toLowerCase();
        return limpio.isEmpty() ? null : limpio;
    }
}
