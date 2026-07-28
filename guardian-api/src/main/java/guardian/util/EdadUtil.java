package guardian.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

/**
 * Calculo de edad. Helper puro, sin estado.
 */
public final class EdadUtil {

    private EdadUtil() {
    }

    /**
     * @return la edad en anos cumplidos, o {@code null} si no hay fecha de
     *         nacimiento registrada. Devolver null y no 0 es deliberado: la
     *         pantalla de la garita debe poder mostrar un guion en vez de
     *         afirmar que la persona tiene cero anos.
     */
    public static Integer calcular(Date fechaNacimiento) {
        if (fechaNacimiento == null) {
            return null;
        }
        LocalDate nacimiento = Instant.ofEpochMilli(fechaNacimiento.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return Period.between(nacimiento, LocalDate.now()).getYears();
    }
}
