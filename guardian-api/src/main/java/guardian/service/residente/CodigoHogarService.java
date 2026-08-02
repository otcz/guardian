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
     * Crea la persona, su vinculo con la casa y su cuenta, y quema el codigo.
     *
     * <p>La cuenta nace con la clave inicial del sistema y cambio obligatorio,
     * igual que cualquier otra. Sin foto: se la sube ella misma desde Mi QR y
     * ahi recibe su credencial.</p>
     */
    void registrar(String codigo, RegistroHogarRequest request);
}
