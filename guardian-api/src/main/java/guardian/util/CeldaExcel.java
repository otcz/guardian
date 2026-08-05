package guardian.util;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

/**
 * Leer una celda como texto, sea del tipo que sea.
 *
 * <p>Existe por un fallo real: {@code getCellAsString} revienta con
 * "Wrong cell type NUMBER, wanted STRING", y Excel guarda como NUMERO todo lo
 * que parezca uno — una cedula, un telefono, el numero de una casa. Como el
 * error salta al leer el archivo, tumbaba la importacion ENTERA y el
 * administrador solo veia "no pudimos leer el archivo" despues de haber llenado
 * la plantilla bien.</p>
 *
 * <p>Es justo lo que pasa con la plantilla que el propio sistema entrega: se
 * abre en Excel, se escribe la cedula, y Excel decide que es un numero.</p>
 */
public final class CeldaExcel {

    private CeldaExcel() {
    }

    public static String texto(Row fila, int columna) {
        Optional<Cell> celda = fila.getOptionalCell(columna);
        if (!celda.isPresent() || celda.get().getValue() == null) {
            return "";
        }

        Cell valor = celda.get();
        if (valor.getType() == CellType.NUMBER) {
            // "1074" y no "1074.0": Excel guarda TODO numero como decimal, y un
            // documento o un telefono con coma decimal no sirven para nada.
            // toPlainString ademas evita la notacion cientifica, en la que un
            // telefono largo se convertiria en "3.001234567E9".
            return ((BigDecimal) valor.getValue()).stripTrailingZeros().toPlainString();
        }

        // String.valueOf y no asString(): asString tambien lanza cuando el tipo
        // no es el que espera, que es el defecto que este helper vino a cerrar.
        return String.valueOf(valor.getValue()).trim();
    }

    /**
     * Una fecha, venga como fecha de Excel o escrita a mano.
     *
     * <p>Excel guarda las fechas como el NUMERO de dias desde 1900, asi que una
     * celda de fecha es indistinguible de una cedula si solo se mira el valor:
     * lo que las separa es el FORMATO de la celda. Sin esa comprobacion, un
     * numero cualquiera en esa columna se convertiria en una fecha creible y
     * nadie lo notaria.</p>
     *
     * @return la fecha, o {@code null} si la celda esta vacia o no se entiende.
     */
    public static LocalDate fecha(Row fila, int columna) {
        Optional<Cell> celda = fila.getOptionalCell(columna);
        if (!celda.isPresent() || celda.get().getValue() == null) {
            return null;
        }

        Cell valor = celda.get();
        if (valor.getType() == CellType.NUMBER) {
            return tieneFormatoDeFecha(valor) ? comoFecha(valor) : null;
        }
        return parsear(String.valueOf(valor.getValue()).trim());
    }

    private static boolean tieneFormatoDeFecha(Cell celda) {
        String formato = celda.getDataFormatString();
        if (formato == null || formato.isEmpty() || "General".equalsIgnoreCase(formato)) {
            return false;
        }
        // Un formato de fecha nombra dias, meses o anios. Basta con encontrarlos:
        // "dd/mm/yyyy", "d-mmm-yy" y "yyyy-mm-dd" caen todos aca.
        String minusculas = formato.toLowerCase(Locale.ROOT);
        return minusculas.indexOf('y') >= 0 || minusculas.indexOf('d') >= 0;
    }

    private static LocalDate comoFecha(Cell celda) {
        try {
            LocalDateTime momento = celda.asDate();
            return momento == null ? null : momento.toLocalDate();
        } catch (RuntimeException fallo) {
            return null;
        }
    }

    /**
     * Texto: la columna quedo formateada como texto, o alguien la escribio a
     * mano. Se aceptan las dos formas que la gente usa de verdad — dd/MM/yyyy,
     * que es como se escribe en el pais, y yyyy-MM-dd, que es lo que sale al
     * exportar desde otro sistema.
     */
    private static LocalDate parsear(String texto) {
        if (texto.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignorado) {
                // Se prueba el siguiente.
            }
        }
        return null;
    }

    private static final DateTimeFormatter[] FORMATOS = {
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };
}
