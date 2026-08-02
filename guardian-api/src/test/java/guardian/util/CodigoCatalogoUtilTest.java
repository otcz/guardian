package guardian.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoCatalogoUtilTest {

    @Test
    @DisplayName("mayusculas y guion bajo en vez de espacios")
    void normalizaEspacios() {
        assertThat(CodigoCatalogoUtil.desde("Camion de mudanza"))
                .isEqualTo("CAMION_DE_MUDANZA");
    }

    @Test
    @DisplayName("quita tildes y n con virgulilla")
    void quitaTildes() {
        // El codigo viaja en URLs y en comparaciones: ahi un caracter no ASCII
        // solo puede causar problemas.
        assertThat(CodigoCatalogoUtil.desde("Camión")).isEqualTo("CAMION");
        assertThat(CodigoCatalogoUtil.desde("Mañana")).isEqualTo("MANANA");
    }

    @Test
    @DisplayName("colapsa la puntuacion y no deja guiones bajos en los extremos")
    void colapsaPuntuacion() {
        assertThat(CodigoCatalogoUtil.desde("  ¡Moto / Scooter!  "))
                .isEqualTo("MOTO_SCOOTER");
    }

    @Test
    @DisplayName("respeta el limite de 40 de la columna, sin terminar en guion bajo")
    void respetaLongitudDeColumna() {
        String largo = CodigoCatalogoUtil.desde(
                "Vehiculo de carga pesada para mudanzas grandes");

        assertThat(largo).hasSizeLessThanOrEqualTo(40);
        assertThat(largo).doesNotEndWith("_");
    }

    @Test
    @DisplayName("un texto sin letras ni numeros no produce codigo")
    void textoSinContenidoDaVacio() {
        // El servicio se apoya en esto para rechazar el alta: un codigo vacio
        // seria una fila imposible de referenciar.
        assertThat(CodigoCatalogoUtil.desde("--- ***")).isEmpty();
        assertThat(CodigoCatalogoUtil.desde(null)).isEmpty();
    }

    @Test
    @DisplayName("dos escrituras del mismo nombre dan el mismo codigo")
    void mismoNombreMismoCodigo() {
        // Es lo que permite reencender una opcion apagada en vez de duplicarla.
        assertThat(CodigoCatalogoUtil.desde("mazda"))
                .isEqualTo(CodigoCatalogoUtil.desde("  MAZDA "));
    }
}
