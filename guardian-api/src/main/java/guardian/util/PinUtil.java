package guardian.util;

import guardian.constant.Codigos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Las reglas de un PIN elegido por una persona.
 *
 * <p>Con cuatro digitos, la fuerza bruta ciega no es la amenaza: el bloqueo por
 * intentos la vuelve cosa de semanas. La amenaza real es que casi todo el mundo
 * elige el mismo punado de numeros. Un vecino con la cedula a la vista prueba
 * 0000, 1234 y el ano de nacimiento, y acierta mas seguido de lo que uno
 * quisiera creer.</p>
 *
 * <p>Por eso lo que se rechaza no es "poca entropia" en abstracto, sino la
 * lista concreta de lo que alguien probaria primero.</p>
 */
public final class PinUtil {

    /**
     * PIN que se rechazan siempre.
     *
     * <p>Los repetidos y las secuencias salen de recorrer los digitos; el resto
     * son los que aparecen arriba en todos los estudios de PIN filtrados —
     * fechas tipo 1990 y 2000, y los patrones que dibujan una figura en el
     * teclado numerico (2580 es la columna del medio de arriba a abajo).</p>
     */
    private static final Set<String> PROHIBIDOS = construirProhibidos();

    private PinUtil() {
    }

    private static Set<String> construirProhibidos() {
        Set<String> prohibidos = new HashSet<>();

        for (int digito = 0; digito <= 9; digito++) {
            // 0000, 1111, ... 9999
            StringBuilder repetido = new StringBuilder();
            for (int i = 0; i < Codigos.PIN_LONGITUD; i++) {
                repetido.append(digito);
            }
            prohibidos.add(repetido.toString());

            // Secuencias en los dos sentidos: 0123..6789 y 9876..3210.
            StringBuilder sube = new StringBuilder();
            StringBuilder baja = new StringBuilder();
            for (int i = 0; i < Codigos.PIN_LONGITUD; i++) {
                sube.append((digito + i) % 10);
                baja.append(((digito - i) % 10 + 10) % 10);
            }
            prohibidos.add(sube.toString());
            prohibidos.add(baja.toString());
        }

        prohibidos.addAll(Arrays.asList(
                "2580", "0852",          // la columna del medio del teclado
                "1379", "9731",          // las cuatro esquinas
                "1998", "1999", "2000",  // anos de nacimiento frecuentes
                "1990", "1991", "1995", "2001", "2020"));

        return Collections.unmodifiableSet(prohibidos);
    }

    /** Exactamente cuatro digitos. Ni letras, ni espacios, ni tres, ni cinco. */
    public static boolean tieneFormaDePin(String pin) {
        if (pin == null || pin.length() != Codigos.PIN_LONGITUD) {
            return false;
        }
        for (char c : pin.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static boolean esTrivial(String pin) {
        return pin != null && PROHIBIDOS.contains(pin);
    }

    /**
     * El PIN sale del documento de su duena.
     *
     * <p>La cedula esta en la ficha de la porteria, en el carnet y en la boca
     * de cualquiera que la haya visto entrar. Un PIN sacado de ahi es publico
     * sin que ella lo sepa. Se miran los cuatro primeros y los cuatro ultimos,
     * que es como la gente lo recorta.</p>
     */
    public static boolean saleDelDocumento(String pin, String documento) {
        if (pin == null || documento == null) {
            return false;
        }
        String soloDigitos = documento.replaceAll("\\D", "");
        if (soloDigitos.length() < Codigos.PIN_LONGITUD) {
            return false;
        }
        String primeros = soloDigitos.substring(0, Codigos.PIN_LONGITUD);
        String ultimos = soloDigitos.substring(soloDigitos.length() - Codigos.PIN_LONGITUD);
        return pin.equals(primeros) || pin.equals(ultimos);
    }
}
