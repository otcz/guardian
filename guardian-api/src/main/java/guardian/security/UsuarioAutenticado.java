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
}
