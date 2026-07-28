package guardian.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "Escribe tu numero de documento")
    private String documento;

    @NotBlank(message = "Escribe tu contrasena")
    private String clave;
}
