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
     * Da de alta la cuenta de una persona, habilitada y con la clave inicial
     * del sistema.
     *
     * <p>Nacia inhabilitada para que nadie pudiera tomarla antes que su dueno,
     * pero el paso de activarla aparte se olvidaba y la persona rebotaba en el
     * login con su clave en la mano. La contrapartida es real y hay que
     * conocerla: entre el alta y el primer ingreso, quien sepa la cedula de esa
     * persona puede entrar con la clave inicial y ponerle otra. Lo que queda
     * para detectarlo es la columna "ultimo ingreso" del panel — si alguien
     * dice que no ha entrado nunca y ahi figura una fecha, la cuenta se tomo.</p>
     */
    UsuarioResponse crear(UsuarioRequest request, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarRol(Long id, String rol, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor);

    /** Devuelve la clave inicial y vuelve a exigir el cambio en el proximo ingreso. */
    UsuarioResponse restablecerClave(Long id, UsuarioAutenticado ejecutor);

    /**
     * Asigna una clave elegida por la administracion.
     *
     * <p>Es lo que hace falta cuando el guardia esta parado en la porteria sin
     * poder entrar: devolver la clave inicial no sirve si es
     * justamente lo que no recuerda, o si esa clave ya se filtro.</p>
     *
     * <p>La cuenta queda con cambio obligatorio en el proximo ingreso. Quien
     * la asigno la conoce, asi que mientras el dueno no la cambie no es
     * realmente suya.</p>
     */
    UsuarioResponse asignarClave(Long id, String claveNueva, UsuarioAutenticado ejecutor);
}
