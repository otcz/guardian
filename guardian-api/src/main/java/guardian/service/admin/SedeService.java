package guardian.service.admin;

import guardian.dto.admin.PersonaRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.sede.SedeRequest;
import guardian.dto.sede.SedeResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * Administracion de sedes. Exclusivo del super administrador.
 *
 * <p><b>La sede sale siempre del token, nunca de un parametro.</b> Por eso
 * "entrar" a una sede emite un token nuevo con esa sede en el claim: todo
 * {@code /api/admin/**} sigue funcionando sin cambiar una sola firma, y no
 * queda ningun endpoint donde alguien pueda olvidar validar el aislamiento.</p>
 */
public interface SedeService {

    List<SedeResponse> listar();

    SedeResponse crear(SedeRequest request, UsuarioAutenticado ejecutor);

    SedeResponse actualizar(Long id, SedeRequest request, UsuarioAutenticado ejecutor);

    SedeResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor);

    /** El primer administrador de una sede. Nace ACTIVO: no hay quien lo habilite. */
    SedeResponse crearAdministrador(Long sedeId, PersonaRequest request,
                                    UsuarioAutenticado ejecutor);

    /** @return sesion nueva con la sede en el token. */
    LoginResponse entrar(Long sedeId, UsuarioAutenticado ejecutor);

    /** @return sesion nueva sin sede: vuelve al panel de plataforma. */
    LoginResponse salir(UsuarioAutenticado ejecutor);

    /**
     * Crea la sede con su porteria. Lo usa tambien el bootstrap: si la
     * creacion desde el panel no sembrara el punto de acceso, los eventos de
     * esa sede quedarian sin decir por donde entro nadie, sin error ni log.
     */
    GdConjunto crearConPorteria(String nombre, SedeRequest datos, String ejecutor);
}
