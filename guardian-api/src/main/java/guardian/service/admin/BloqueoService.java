package guardian.service.admin;

import guardian.security.UsuarioAutenticado;

/**
 * La llave del ADMINISTRADOR, y solo suya.
 *
 * <p>Vive aparte de los {@code cambiarEstado} de cada service porque son dos
 * decisiones de dueno distinto: el residente enciende y apaga lo suyo, la
 * administracion bloquea. Mezclarlas en el mismo metodo fue justo lo que
 * permitia que el residente deshiciera con un toque lo que el conjunto habia
 * decidido.</p>
 *
 * <p>Bloqueado gana siempre: nada con la llave del admin abajo puede entrar ni
 * salir del conjunto, aunque su dueno lo tenga encendido.</p>
 */
public interface BloqueoService {

    void bloquearPersona(Long id, String motivo, UsuarioAutenticado admin);

    void desbloquearPersona(Long id, UsuarioAutenticado admin);

    void bloquearVehiculo(Long id, String motivo, UsuarioAutenticado admin);

    void desbloquearVehiculo(Long id, UsuarioAutenticado admin);

    void bloquearCasa(Long id, String motivo, UsuarioAutenticado admin);

    void desbloquearCasa(Long id, UsuarioAutenticado admin);

    /** Corta el acceso de un guardia o de cualquier cuenta, sesion viva incluida. */
    void bloquearUsuario(Long id, String motivo, UsuarioAutenticado admin);

    void desbloquearUsuario(Long id, UsuarioAutenticado admin);
}
