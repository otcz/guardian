package guardian.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta del paso 1. Deliberadamente pobre, y siempre la misma.
 *
 * <p>NO dice si el documento existe, si tiene cuenta, ni a que correo se envio
 * el codigo. Cualquiera de esas tres cosas convertiria esta pantalla en un
 * verificador de quien vive en el conjunto, abierto sin sesion a todo el mundo
 * — y el dato que se estaria confirmando es una cedula.</p>
 *
 * <p>Se penso en devolver una pista del buzon ("a***@gmail.com") para ayudar a
 * quien tiene varios correos. Se descarto: mandarla solo cuando la cuenta
 * existe ES la fuga que se queria evitar, porque un campo nulo responde la
 * pregunta igual de bien que uno lleno. Quien SI es el dueno abre su correo y
 * busca un mensaje de GUARDIAN; no necesita que le digamos en cual buzon.</p>
 */
@Data
@Builder
public class SolicitudCodigoResponse {

    /** Siempre el mismo texto, exista o no la cuenta. */
    private String mensaje;

    private int minutosVigencia;
}
