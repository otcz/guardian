package guardian.service.huella;

import guardian.dto.huella.EstadoHuellaResponse;
import guardian.dto.huella.HuellasDeUnaPersonaResponse;
import guardian.dto.huella.RegistrarHuellaRequest;
import guardian.security.UsuarioAutenticado;

/**
 * Enrolar y borrar huellas. El cotejo vive en {@link CotejadorHuellas}.
 *
 * <p>Todo lo de aca es logica de negocio pura y no depende del lector: cuantos
 * dedos puede tener una persona, quien puede enrolar, que pasa si ya existe ese
 * dedo. Se construye y se prueba sin hardware.</p>
 */
public interface HuellaService {

    /** Si el modulo esta operativo, y con que algoritmo. Lo consulta la pantalla. */
    EstadoHuellaResponse estado();

    /** Que dedos tiene registrados una persona, para no ofrecer el mismo dos veces. */
    HuellasDeUnaPersonaResponse dePersona(Long personaId, UsuarioAutenticado guardia);

    /**
     * Guarda un dedo a partir de sus lecturas.
     *
     * <p>Reemplaza si ese dedo ya estaba: quien vuelve a enrolar el mismo dedo
     * es porque el anterior no le funciona, y dejar los dos solo hace mas lento
     * el cotejo sin hacerlo mas certero.</p>
     */
    HuellasDeUnaPersonaResponse registrar(RegistrarHuellaRequest request,
                                          UsuarioAutenticado guardia);

    /** Borra un dedo. La persona sigue entrando por codigo o por documento. */
    HuellasDeUnaPersonaResponse eliminar(Long personaId, String dedo,
                                         UsuarioAutenticado guardia);
}
