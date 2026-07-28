package guardian.service.auth;

import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.security.UsuarioAutenticado;

public interface AutenticacionService {

    LoginResponse login(LoginRequest request);

    void cambiarClave(UsuarioAutenticado usuario, CambiarClaveRequest request);

    SesionResponse sesionActual(UsuarioAutenticado usuario);
}
