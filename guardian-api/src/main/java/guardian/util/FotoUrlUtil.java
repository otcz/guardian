package guardian.util;

import guardian.constant.ApiEndpoint;

import java.util.regex.Pattern;

/**
 * Valida que una foto sea una subida por la propia aplicacion.
 *
 * <p>Aceptar una URL arbitraria dejaria colar una imagen externa —o un enlace
 * roto— en el control de seguridad central del sistema: lo que el guardia
 * compara contra la cara que tiene enfrente.</p>
 *
 * <p>Vive aca y no dentro de un service porque ahora hay DOS caminos que
 * asignan foto: el administrador desde el panel y el residente desde su
 * propia pantalla. Con la regla copiada en los dos, el dia que se afloje en
 * uno queda un hueco silencioso en el otro.</p>
 */
public final class FotoUrlUtil {

    private static final Pattern VALIDA =
            Pattern.compile(Pattern.quote(ApiEndpoint.PUBLICO_FOTOS)
                    + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                    + "\\.[a-z0-9]{2,5}");

    private FotoUrlUtil() {
    }

    /** Vacio o nulo NO es invalido: significa "sin foto", que es un estado legitimo. */
    public static boolean esValida(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.trim().isEmpty()) {
            return true;
        }
        return VALIDA.matcher(fotoUrl.trim()).matches();
    }

    public static boolean tieneFoto(String fotoUrl) {
        return fotoUrl != null && !fotoUrl.trim().isEmpty();
    }

    /**
     * El nombre del archivo dentro de una ruta de foto propia, o null.
     *
     * <p>Devuelve null —y no la cadena cruda— para todo lo que no sea una
     * subida de esta aplicacion. Es deliberado: quien llama a esto lo usa para
     * BORRAR del disco, y una ruta ajena no puede llegar nunca a resolverse
     * contra el sistema de archivos.</p>
     */
    public static String nombreArchivoDe(String fotoUrl) {
        if (!tieneFoto(fotoUrl) || !esValida(fotoUrl)) {
            return null;
        }
        return fotoUrl.trim().substring((ApiEndpoint.PUBLICO_FOTOS + "/").length());
    }
}
