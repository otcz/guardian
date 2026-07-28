package guardian.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Firma HMAC-SHA256 en Base64 URL-safe. Helper puro, sin estado.
 */
public final class HmacUtil {

    private static final String ALGORITMO = "HmacSHA256";

    private HmacUtil() {
    }

    public static String firmar(String contenido, String secreto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] firma = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);

        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC", ex);
        }
    }

    /**
     * Comparacion en tiempo constante.
     *
     * <p>Un {@code equals} normal corta en el primer byte distinto, y esa
     * diferencia de microsegundos deja medir cuantos caracteres del principio
     * acerto un atacante. Repitiendo la medicion se reconstruye la firma byte a
     * byte sin conocer nunca la clave.</p>
     */
    public static boolean coincide(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] bytesA = a.getBytes(StandardCharsets.UTF_8);
        byte[] bytesB = b.getBytes(StandardCharsets.UTF_8);

        int diferencia = bytesA.length ^ bytesB.length;
        for (int i = 0; i < bytesA.length && i < bytesB.length; i++) {
            diferencia |= bytesA[i] ^ bytesB[i];
        }
        return diferencia == 0;
    }
}
