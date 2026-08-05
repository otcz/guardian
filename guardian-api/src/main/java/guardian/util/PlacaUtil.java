package guardian.util;

/**
 * La placa, siempre escrita igual.
 *
 * <p>Mayusculas y sin espacios ni guiones. Sin esto "abc 123", "ABC-123" y
 * "ABC123" serian tres vehiculos distintos, y el guardia que no encuentra la
 * placa en la lista termina dejando pasar sin registrar.</p>
 *
 * <p>Vive aca y no dentro de un service porque hoy la normalizan tres caminos
 * —el alta del administrador, la solicitud del residente y su aprobacion— y
 * duplicada se corrige en uno y se olvida en los otros.</p>
 */
public final class PlacaUtil {

    private PlacaUtil() {
    }

    public static String normalizar(String placa) {
        if (placa == null) {
            return null;
        }
        return placa.trim().toUpperCase().replaceAll("[\\s-]", "");
    }
}
