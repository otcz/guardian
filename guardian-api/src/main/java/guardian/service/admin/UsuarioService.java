package guardian.service.admin;

import guardian.dto.admin.UsuarioRequest;
import guardian.dto.admin.UsuarioResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

public interface UsuarioService {

    /**
     * Cuentas de la sede, SIN la del propio ejecutor: un administrador no se
     * gestiona a si mismo desde el panel — su clave la cambia en "Mi cuenta".
     */
    List<UsuarioResponse> listar(UsuarioAutenticado ejecutor);

    /**
     * Da de alta la cuenta de una persona con clave inicial igual a su documento.
     *
     * <p>Nace <b>inactiva</b>: si naciera activa, cualquiera que conozca la
     * cedula de un vecino podria tomar la cuenta antes que su dueno. El
     * administrador la habilita cuando entrega el acceso en mano.</p>
     */
    UsuarioResponse crear(UsuarioRequest request, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarRol(Long id, String rol, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor);

    /** Devuelve la clave al documento y vuelve a exigir el cambio en el proximo ingreso. */
    UsuarioResponse restablecerClave(Long id, UsuarioAutenticado ejecutor);

    /**
     * Asigna una clave elegida por la administracion.
     *
     * <p>Es lo que hace falta cuando el guardia esta parado en la porteria sin
     * poder entrar: restablecer al documento no sirve si el documento es
     * justamente lo que no recuerda, o si esa clave ya se filtro.</p>
     *
     * <p>La cuenta queda con cambio obligatorio en el proximo ingreso. Quien
     * la asigno la conoce, asi que mientras el dueno no la cambie no es
     * realmente suya.</p>
     */
    UsuarioResponse asignarClave(Long id, String claveNueva, UsuarioAutenticado ejecutor);
}
