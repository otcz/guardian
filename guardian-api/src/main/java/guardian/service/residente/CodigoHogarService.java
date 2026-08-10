package guardian.service.residente;

import guardian.dto.residente.CodigoHogarResponse;
import guardian.dto.residente.HogarPublicoResponse;
import guardian.dto.residente.RegistroHogarRequest;
import guardian.security.UsuarioAutenticado;

/**
 * La via por la que un nucleo familiar se arma solo: el titular genera un
 * codigo, se lo pasa a su familiar, y ese familiar crea su cuenta sin que el
 * administrador tenga que digitarlo.
 */
public interface CodigoHogarService {

    /** Genera uno nuevo e invalida el anterior. Solo el titular de la casa. */
    CodigoHogarResponse generar(UsuarioAutenticado titular);

    /** El codigo vivo de mi hogar, o null si no hay ninguno. */
    CodigoHogarResponse vigente(UsuarioAutenticado usuario);

    void revocar(UsuarioAutenticado titular);

    /**
     * Lo que ve quien abre el enlace, SIN sesion. Solo conjunto, casa y
     * titular: lo justo para saber a donde se une.
     */
    HogarPublicoResponse consultar(String codigo);

    /**
     * Guarda los datos como una solicitud PENDIENTE. No crea la persona, su
     * cuenta ni su vinculo con la casa — eso lo hace
     * {@link guardian.service.admin.SolicitudHogarAdminService#aprobar} cuando
     * un administrador decide. El codigo sigue sin quemarse hasta entonces.
     */
    void registrar(String codigo, RegistroHogarRequest request);
}
