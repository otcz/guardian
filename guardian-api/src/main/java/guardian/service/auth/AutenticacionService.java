package guardian.service.auth;

import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.security.UsuarioAutenticado;

public interface AutenticacionService {

    LoginResponse login(LoginRequest request);

    /**
     * @return una sesion NUEVA con token fresco. El token anterior lleva la
     *         autoridad degradada CLAVE_PENDIENTE; sin reemplazarlo, el usuario
     *         quedaria bloqueado justo despues de hacer lo correcto.
     */
    LoginResponse cambiarClave(UsuarioAutenticado usuario, CambiarClaveRequest request);

    SesionResponse sesionActual(UsuarioAutenticado usuario);
}
