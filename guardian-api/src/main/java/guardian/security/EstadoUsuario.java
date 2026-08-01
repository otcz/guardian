package guardian.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Foto instantanea del estado de un usuario, leida de la base y cacheada unos
 * segundos. Es lo que evita que el JWT sea la unica verdad durante 12 horas.
 */
@Getter
@AllArgsConstructor
public class EstadoUsuario {

    /** false si el usuario o su persona fueron deshabilitados. */
    private final boolean puedeOperar;

    /** Rol vigente en base, no el que decia el token al emitirse. */
    private final String rol;

    private final boolean cambioClavePendiente;
}
