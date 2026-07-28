package guardian.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepcion de dominio. Lleva el estado HTTP para que el handler global no
 * tenga que adivinarlo a partir del tipo.
 */
@Getter
public class GuardianException extends RuntimeException {

    private final HttpStatus estado;

    public GuardianException(String mensaje, HttpStatus estado) {
        super(mensaje);
        this.estado = estado;
    }

    public static GuardianException solicitudInvalida(String mensaje) {
        return new GuardianException(mensaje, HttpStatus.BAD_REQUEST);
    }

    public static GuardianException noAutorizado(String mensaje) {
        return new GuardianException(mensaje, HttpStatus.UNAUTHORIZED);
    }

    public static GuardianException sinPermiso(String mensaje) {
        return new GuardianException(mensaje, HttpStatus.FORBIDDEN);
    }

    public static GuardianException noEncontrado(String mensaje) {
        return new GuardianException(mensaje, HttpStatus.NOT_FOUND);
    }

    public static GuardianException conflicto(String mensaje) {
        return new GuardianException(mensaje, HttpStatus.CONFLICT);
    }
}
