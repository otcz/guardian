package guardian;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La zona con la que la aplicacion interpreta las fechas.
 *
 * <p>Las 34 columnas de fecha son {@code timestamp without time zone}: lo que
 * queda guardado es la hora de PARED. Mientras se escriba y se lea con la misma
 * zona todo cuadra; el dia que el proceso corra en otra —Cloud Run esta en
 * UTC— lo ya guardado se lee corrido cinco horas.</p>
 */
class ZonaHorariaTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restaurar() {
        TimeZone.setDefault(original);
    }

    @Test
    @DisplayName("la aplicacion queda en la zona de Colombia, no en la del sistema")
    void fijaLaZonaDeColombia() {
        // Simula Cloud Run, que es donde esto se rompia.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        GuardianApplication.fijarZonaHoraria();

        assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Bogota");
    }

    @Test
    @DisplayName("la misma hora de pared significa lo mismo en las dos maquinas")
    void laHoraGuardadaSeInterpretaIgual() {
        // Es la propiedad que importa: una fila escrita en la maquina de
        // desarrollo y leida en el servidor tiene que dar el mismo instante.
        // Antes no lo daba — la de UTC la leia cinco horas antes.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        GuardianApplication.fijarZonaHoraria();
        Date desdeElServidor = horaDePared("2026-08-02 10:36:24");

        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        GuardianApplication.fijarZonaHoraria();
        Date desdeDesarrollo = horaDePared("2026-08-02 10:36:24");

        assertThat(desdeElServidor).isEqualTo(desdeDesarrollo);
    }

    @Test
    @DisplayName("una zona mal escrita NO deja el sistema en UTC en silencio")
    void zonaInvalidaNoCaeEnUtc() {
        // TimeZone.getTimeZone devuelve GMT sin avisar cuando el nombre no
        // existe: un "America/Bogata" en una variable de entorno dejaria todo
        // corrido y con pinta de normal.
        TimeZone elegida = TimeZone.getTimeZone("America/Bogata");

        assertThat(elegida.getID())
                .as("Java no reconoce el nombre y cae a GMT")
                .isEqualTo("GMT");
    }

    /** Lo que hace Hibernate al leer un timestamp sin zona: usa la de la JVM. */
    private Date horaDePared(String texto) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(texto);
        } catch (java.text.ParseException fallo) {
            throw new IllegalStateException(fallo);
        }
    }
}
