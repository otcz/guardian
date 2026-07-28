package guardian.service.acceso;

import guardian.dto.residente.MiQrResponse;
import guardian.security.UsuarioAutenticado;

public interface MiCredencialService {

    /**
     * Credencial del residente en sesion. Si todavia no tiene una activa, se
     * emite en el momento: obligar al administrador a generarla a mano dejaria
     * al residente con una pantalla vacia el dia que estrena la aplicacion.
     */
    MiQrResponse miQr(UsuarioAutenticado usuario);

    byte[] miQrPng(UsuarioAutenticado usuario, int tamanoPx);
}
