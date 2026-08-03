package guardian.security;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    // ── Quien puede nombrar a quien ──────────────────────────────────────────

    @Test
    @DisplayName("un ADMIN no puede nombrar a otro ADMIN")
    void elAdminNoNombraAdmins() {
        // Es el punto de toda la regla. Un administrador que puede crear otro
        // convierte una cuenta comprometida en varias: quien entra se deja un
        // segundo administrador puesto, y quitarle el acceso al primero ya no
        // cierra nada.
        assertThatThrownBy(() ->
                Autoridad.exigirRolAsignablePor(con(Codigos.ROL_ADMIN), Codigos.ROL_ADMIN))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.ADMIN_SOLO_SUPER_ADMIN);
    }

    @Test
    @DisplayName("el SUPER_ADMIN si nombra administradores")
    void elSuperAdminSiNombraAdmins() {
        assertThatCode(() ->
                Autoridad.exigirRolAsignablePor(con(Codigos.ROL_SUPER_ADMIN), Codigos.ROL_ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SUPER_ADMIN no lo asigna NADIE, ni el mismo")
    void nadieNombraSuperAdmins() {
        // No basta con no ponerlo en el catalogo: el dia que alguien lo agregue
        // "para que aparezca en el combo", esto es lo unico que impide que se
        // reparta desde un PATCH.
        for (String quien : new String[]{Codigos.ROL_SUPER_ADMIN, Codigos.ROL_ADMIN}) {
            assertThatThrownBy(() ->
                    Autoridad.exigirRolAsignablePor(con(quien), Codigos.ROL_SUPER_ADMIN))
                    .as("ejecutor %s", quien)
                    .hasMessage(MensajesGlobales.ROL_NO_ASIGNABLE);
        }
    }

    @Test
    @DisplayName("guardia y residente los reparte cualquiera de los dos")
    void losRolesDeAbajoNoSeRestringen() {
        for (String quien : new String[]{Codigos.ROL_SUPER_ADMIN, Codigos.ROL_ADMIN}) {
            for (String rol : new String[]{Codigos.ROL_GUARDIA, Codigos.ROL_RESIDENTE}) {
                assertThatCode(() -> Autoridad.exigirRolAsignablePor(con(quien), rol))
                        .as("%s nombrando %s", quien, rol)
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    @DisplayName("la lista del combo coincide con lo que el guardia deja pasar")
    void elComboNoOfreceLoQueElGuardiaRechaza() {
        // Si divergen, el administrador elige una opcion, llena el formulario
        // entero y recibe un 403 al guardar.
        for (String quien : new String[]{Codigos.ROL_SUPER_ADMIN, Codigos.ROL_ADMIN}) {
            for (String rol : Autoridad.rolesAsignablesPor(con(quien))) {
                assertThatCode(() -> Autoridad.exigirRolAsignablePor(con(quien), rol))
                        .as("%s ofrece %s", quien, rol)
                        .doesNotThrowAnyException();
            }
        }
        assertThat(Autoridad.rolesAsignablesPor(con(Codigos.ROL_ADMIN)))
                .doesNotContain(Codigos.ROL_ADMIN, Codigos.ROL_SUPER_ADMIN);
    }

    private UsuarioAutenticado con(String rol) {
        return new UsuarioAutenticado(1L, 10L, 1L, "123", "Alguien", rol);
    }
}
