package guardian.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class EdadUtilTest {

    @Test
    @DisplayName("sin fecha de nacimiento devuelve null, no cero")
    void sinFechaDevuelveNull() {
        // La pantalla de la garita muestra un guion cuando no hay dato.
        // Un 0 afirmaria que la persona tiene cero anos.
        assertThat(EdadUtil.calcular(null)).isNull();
    }

    @Test
    @DisplayName("calcula anos cumplidos")
    void calculaAnosCumplidos() {
        assertThat(EdadUtil.calcular(haceAnos(30))).isEqualTo(30);
    }

    @Test
    @DisplayName("el dia anterior al cumpleanos todavia no suma el ano")
    void visperaDelCumpleanos() {
        LocalDate manana = LocalDate.now().plusDays(1).minusYears(30);
        assertThat(EdadUtil.calcular(aDate(manana))).isEqualTo(29);
    }

    @Test
    @DisplayName("el mismo dia del cumpleanos ya suma el ano")
    void diaDelCumpleanos() {
        assertThat(EdadUtil.calcular(aDate(LocalDate.now().minusYears(30)))).isEqualTo(30);
    }

    private Date haceAnos(int anos) {
        return aDate(LocalDate.now().minusYears(anos).minusDays(1));
    }

    private Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
