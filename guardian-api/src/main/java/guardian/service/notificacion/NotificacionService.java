package guardian.service.notificacion;

import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdSolicitudHogar;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.entity.vehiculo.GdVehiculo;

/**
 * Los avisos por correo de lo que la administracion decide.
 *
 * <p><b>Por que existe.</b> Hasta ahora todo lo que resolvia la administracion
 * era de tipo "pull": el residente tenia que abrir la aplicacion para enterarse
 * de que su vehiculo quedo autorizado, o de por que su carro dejo de entrar. Si
 * no entraba, no se enteraba — y el caso que mas duele es justamente el que
 * menos se mira: un rechazo o un bloqueo dejan a alguien sin poder pasar por la
 * porteria sin saber por que.</p>
 *
 * <p><b>Ningun metodo lanza.</b> Quien llama a esto ya aprobo, rechazo o
 * bloqueo: la operacion esta hecha y el aviso es un extra. Un SMTP caido no
 * puede dejar al conjunto sin poder administrarse.</p>
 *
 * <p><b>Todo sale despues de confirmar la transaccion.</b> Avisar "tu vehiculo
 * quedo autorizado" dentro de una transaccion que despues revierte es peor que
 * no avisar: el correo no se puede retirar.</p>
 */
public interface NotificacionService {

    /** Al titular que la pidio. El vehiculo ya existe si fue aprobada. */
    void solicitudVehiculoResuelta(GdSolicitudVehiculo solicitud, boolean aprobada);

    /** A quien pidio entrar al hogar, que es quien esta esperando respuesta. */
    void solicitudHogarResuelta(GdSolicitudHogar solicitud, boolean aprobada);

    /**
     * Al anfitrion, que NO siempre es el titular: cualquier residente invita, y
     * quien tiene que avisarle a su visita es el que la invito.
     */
    void invitacionResuelta(GdInvitacion invitacion, boolean aprobada);

    /**
     * Una persona quedo deshabilitada o habilitada de nuevo.
     *
     * <p>Le llega a ella —es quien va a encontrarse la talanquera abajo— y al
     * titular de su casa, que es quien responde por el hogar.</p>
     */
    void bloqueoPersonaCambiado(GdPersona persona, boolean bloqueada, String motivo);

    /** Un vehiculo quedo deshabilitado o habilitado. Le llega al titular. */
    void bloqueoVehiculoCambiado(GdVehiculo vehiculo, boolean bloqueado, String motivo);

    /** El correo del titular de una casa, o null si no tiene o no hay titular. */
    String correoDelTitular(GdCasa casa);
}
