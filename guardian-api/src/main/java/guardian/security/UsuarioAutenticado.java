package guardian.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Identidad del llamante, resuelta del JWT. Es lo que los services reciben para
 * saber quien esta actuando y sobre que conjunto.
 */
@Getter
@AllArgsConstructor
public class UsuarioAutenticado {

    private final Long usuarioId;
    private final Long personaId;
    private final Long conjuntoId;
    private final String documento;
    private final String nombreCompleto;
    private final String rol;

    /**
     * true mientras la clave siga siendo la inicial. Con este flag el filtro
     * degrada la autoridad a {@code CLAVE_PENDIENTE}: el cambio obligatorio se
     * aplica en el backend, no solo en la pantalla.
     */
    private final boolean cambioClavePendiente;

    /**
     * true cuando un super administrador esta operando DENTRO de una sede.
     *
     * <p>Sin esta marca, un cambio hecho por el operador de la plataforma
     * queda en los campos de auditoria indistinguible de uno hecho por el
     * administrador local, y en una disputa entre ambos no hay forma de
     * reconstruir quien lo hizo.</p>
     */
    private final boolean sedeSuplantada;

    public UsuarioAutenticado(Long usuarioId, Long personaId, Long conjuntoId,
                              String documento, String nombreCompleto, String rol) {
        this(usuarioId, personaId, conjuntoId, documento, nombreCompleto, rol, false, false);
    }

    public UsuarioAutenticado(Long usuarioId, Long personaId, Long conjuntoId,
                              String documento, String nombreCompleto, String rol,
                              boolean cambioClavePendiente) {
        this(usuarioId, personaId, conjuntoId, documento, nombreCompleto, rol,
                cambioClavePendiente, false);
    }
}
