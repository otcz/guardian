package guardian.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private SesionResponse usuario;

    /**
     * Cuando viene en {@code true} el frontend debe llevar al usuario a cambiar
     * la clave antes que a cualquier otra pantalla.
     *
     * <p>El token se entrega igual, porque el endpoint de cambio de clave
     * tambien exige autenticacion. Lo que bloquea el resto de la aplicacion es
     * el guard del frontend, no la ausencia de token.</p>
     */
    private boolean requiereCambioClave;
}
