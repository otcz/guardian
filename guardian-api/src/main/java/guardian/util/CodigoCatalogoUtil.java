package guardian.util;

import java.text.Normalizer;

/**
 * Deriva el codigo estable de una opcion del catalogo a partir de su texto.
 *
 * <p>El administrador escribe "Camión de mudanza" y la fila queda con codigo
 * {@code CAMION_DE_MUDANZA}. Pedirle el codigo aparte seria pedirle que invente
 * una llave tecnica, y de ahi salen los "OPCION_2" y los codigos con tilde que
 * despues nadie puede referenciar desde el codigo fuente.</p>
 *
 * <p>Sin tildes ni ñ a proposito: el codigo viaja en URLs, en JSON y en
 * comparaciones, y ahi un caracter no ASCII solo puede causar problemas.</p>
 */
public final class CodigoCatalogoUtil {

    /** GD_PARAMETRO.CODIGO es varchar(40). */
    private static final int LONGITUD_MAXIMA = 40;

    private CodigoCatalogoUtil() {
    }

    public static String desde(String valor) {
        if (valor == null) {
            return "";
        }

        // NFD separa la letra de su tilde; el filtro de rango borra las tildes
        // sueltas y deja la letra base. "Camión" -> "Camion".
        String plano = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .replace("ñ", "n")
                .replace("Ñ", "N");

        String codigo = plano.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (codigo.length() > LONGITUD_MAXIMA) {
            codigo = codigo.substring(0, LONGITUD_MAXIMA).replaceAll("_+$", "");
        }
        return codigo;
    }
}
