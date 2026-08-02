package guardian.security;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La regla que separa desactivar de borrar.
 *
 * <p>Desactivar, bloquear y revocar dejan rastro y son del administrador de la
 * sede. Borrar no lo deja, y quien borra suele ser parte de la disputa que ese
 * registro resolveria — por eso queda del lado del operador de la
 * plataforma.</p>
 */
class AutoridadTest {

    @Test
    @DisplayName("el super administrador pasa")
    void superAdminPasa() {
        assertThatCode(() -> Autoridad.exigirSuperAdmin(con(Codigos.ROL_SUPER_ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ningun otro rol pasa, y el mensaje dice que hacer en su lugar")
    void losDemasNoPasan() {
        for (String rol : new String[]{Codigos.ROL_ADMIN, Codigos.ROL_GUARDIA,
                Codigos.ROL_RESIDENTE, Codigos.ROL_CLAVE_PENDIENTE}) {
            assertThatThrownBy(() -> Autoridad.exigirSuperAdmin(con(rol)))
                    .as("rol %s", rol)
                    .isInstanceOf(GuardianException.class)
                    .hasMessage(MensajesGlobales.BORRADO_SOLO_SUPER_ADMIN);
        }
    }

    @Test
    @DisplayName("sin identidad tampoco pasa: la ausencia de rol no es permiso")
    void sinIdentidadNoPasa() {
        assertThatThrownBy(() -> Autoridad.exigirSuperAdmin(null))
                .isInstanceOf(GuardianException.class);
        assertThatThrownBy(() -> Autoridad.exigirSuperAdmin(con(null)))
                .isInstanceOf(GuardianException.class);
    }

    private UsuarioAutenticado con(String rol) {
        return new UsuarioAutenticado(1L, 10L, 1L, "123", "Alguien", rol);
    }
}
