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

    /**
     * El residente pone su propia foto y con eso se emite su credencial.
     *
     * <p>Sin esto, alguien registrado sin foto quedaba en un callejon: veia un
     * aviso rojo en su pantalla y dependia de que el administrador se acordara
     * de subirsela.</p>
     */
    MiQrResponse fijarMiFoto(UsuarioAutenticado usuario, String fotoUrl);

    byte[] miQrPng(UsuarioAutenticado usuario, int tamanoPx);
}
