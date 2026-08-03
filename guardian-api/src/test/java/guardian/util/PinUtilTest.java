package guardian.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las reglas del PIN.
 *
 * <p>Con cuatro digitos la fuerza bruta ciega no es la amenaza —el bloqueo por
 * intentos la vuelve cosa de semanas—: la amenaza es que casi todo el mundo
 * elige el mismo punado de numeros, y un vecino con la cedula a la vista
 * prueba justo esos.</p>
 */
class PinUtilTest {

    @Test
    @DisplayName("exactamente cuatro digitos: ni letras, ni tres, ni cinco")
    void formaDePin() {
        assertThat(PinUtil.tieneFormaDePin("5847")).isTrue();

        assertThat(PinUtil.tieneFormaDePin("584")).isFalse();
        assertThat(PinUtil.tieneFormaDePin("58471")).isFalse();
        assertThat(PinUtil.tieneFormaDePin("58a7")).isFalse();
        assertThat(PinUtil.tieneFormaDePin("58 7")).isFalse();
        assertThat(PinUtil.tieneFormaDePin("")).isFalse();
        assertThat(PinUtil.tieneFormaDePin(null)).isFalse();
    }

    @Test
    @DisplayName("el PIN inicial 0000 NO se puede elegir como propio")
    void elInicialNoSePuedeElegir() {
        // Antes lo impedia la longitud minima de ocho. Con PIN de cuatro,
        // 0000 pasa a tener forma valida — y sin esta regla el "cambio
        // obligatorio" del primer ingreso se resolveria escribiendo lo mismo.
        assertThat(PinUtil.esTrivial("0000")).isTrue();
    }

    @Test
    @DisplayName("repetidos y secuencias en los dos sentidos")
    void repetidosYSecuencias() {
        for (String repetido : new String[]{"1111", "5555", "9999"}) {
            assertThat(PinUtil.esTrivial(repetido)).as(repetido).isTrue();
        }
        for (String seguido : new String[]{"1234", "0123", "6789", "9876", "3210"}) {
            assertThat(PinUtil.esTrivial(seguido)).as(seguido).isTrue();
        }
    }

    @Test
    @DisplayName("los patrones del teclado y los anos de nacimiento")
    void patronesConocidos() {
        assertThat(PinUtil.esTrivial("2580")).as("columna del medio").isTrue();
        assertThat(PinUtil.esTrivial("1379")).as("las cuatro esquinas").isTrue();
        assertThat(PinUtil.esTrivial("2000")).as("ano frecuente").isTrue();
    }

    @Test
    @DisplayName("un PIN corriente si pasa")
    void unoNormalPasa() {
        for (String bueno : new String[]{"5847", "3092", "7261", "4806"}) {
            assertThat(PinUtil.esTrivial(bueno)).as(bueno).isFalse();
        }
    }

    @Test
    @DisplayName("el PIN no puede salir del documento, ni por delante ni por detras")
    void noSaleDelDocumento() {
        // La cedula esta en la ficha de la porteria, en el carnet y en la boca
        // de cualquiera que la haya visto entrar.
        assertThat(PinUtil.saleDelDocumento("1020", "1020304050")).isTrue();
        assertThat(PinUtil.saleDelDocumento("4050", "1020304050")).isTrue();

        assertThat(PinUtil.saleDelDocumento("5847", "1020304050")).isFalse();
        // Los del medio no se miran: es lo que la gente recorta, no lo que
        // extrae del centro.
        assertThat(PinUtil.saleDelDocumento("2030", "1020304050")).isFalse();
    }

    @Test
    @DisplayName("un documento no numerico no rompe la comprobacion")
    void documentoNoNumerico() {
        // SUPERADMIN es un documento valido en este sistema.
        assertThat(PinUtil.saleDelDocumento("5847", "SUPERADMIN")).isFalse();
        assertThat(PinUtil.saleDelDocumento("5847", "AB1")).isFalse();
        assertThat(PinUtil.saleDelDocumento("5847", null)).isFalse();
    }
}
