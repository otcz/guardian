package guardian.service.auth;

import guardian.exception.GuardianException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Freno de fuerza bruta: umbral, limpieza y llave insensible a mayusculas. */
class IntentosLoginServiceImplTest {

    private IntentosLoginServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new IntentosLoginServiceImpl(3, 15);
    }

    @Test
    @DisplayName("por debajo del umbral no bloquea")
    void noBloqueaBajoElUmbral() {
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");

        assertThatCode(() -> servicio.exigirNoBloqueado("123")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("al llegar al umbral bloquea")
    void bloqueaAlLlegarAlUmbral() {
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");

        assertThatThrownBy(() -> servicio.exigirNoBloqueado("123"))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("el login exitoso limpia el contador")
    void limpiarDesbloquea() {
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");

        servicio.limpiar("123");

        assertThatCode(() -> servicio.exigirNoBloqueado("123")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la llave ignora mayusculas, igual que el login")
    void llaveIgnoraMayusculas() {
        servicio.registrarFallo("admin");
        servicio.registrarFallo("ADMIN");
        servicio.registrarFallo("Admin ");

        assertThatThrownBy(() -> servicio.exigirNoBloqueado("aDmIn"))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("los fallos de un documento no afectan a otro")
    void contadoresIndependientes() {
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");
        servicio.registrarFallo("123");

        assertThatCode(() -> servicio.exigirNoBloqueado("456")).doesNotThrowAnyException();
    }
}
