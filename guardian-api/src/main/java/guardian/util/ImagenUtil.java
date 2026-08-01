package guardian.util;

import java.nio.charset.StandardCharsets;

/**
 * Verificacion del contenido real de una imagen por sus primeros bytes.
 * Helper puro, sin estado.
 *
 * <p>La extension del nombre la elige el cliente y no prueba nada: un .png
 * puede ser un HTML con scripts que el navegador ejecutaria al abrir la
 * "foto". Los magic bytes si son del archivo.</p>
 */
public final class ImagenUtil {

    private ImagenUtil() {
    }

    public static boolean contenidoCoincide(byte[] datos, String extension) {
        if (datos == null || extension == null) {
            return false;
        }
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return esJpeg(datos);
            case "png":
                return esPng(datos);
            case "webp":
                return esWebp(datos);
            default:
                return false;
        }
    }

    private static boolean esJpeg(byte[] d) {
        return d.length >= 3
                && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    private static boolean esPng(byte[] d) {
        byte[] firma = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (d.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (d[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    /** RIFF....WEBP — los bytes 4-7 son el tamano y no se comparan. */
    private static boolean esWebp(byte[] d) {
        if (d.length < 12) {
            return false;
        }
        String riff = new String(d, 0, 4, StandardCharsets.US_ASCII);
        String webp = new String(d, 8, 4, StandardCharsets.US_ASCII);
        return "RIFF".equals(riff) && "WEBP".equals(webp);
    }
}
