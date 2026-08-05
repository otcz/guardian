package guardian.util;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

import java.math.BigDecimal;
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
}
