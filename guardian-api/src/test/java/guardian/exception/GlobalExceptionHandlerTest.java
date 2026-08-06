package guardian.exception;

import guardian.constant.MensajesGlobales;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que un error del CLIENTE no salga como 500.
 *
 * <p>Importa mas de lo que parece: un 500 dice "el servidor se rompio", levanta
 * alertas de infraestructura por algo que no lo es, y deja un stack trace
 * completo en el log por cada peticion basura. En un endpoint publico eso es
 * todas las que quiera mandar un escaner.</p>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("un cuerpo JSON ilegible es 400, no 500")
    void cuerpoIlegible() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarPeticionIlegible(
                new HttpMessageNotReadableException("Invalid UTF-8 middle byte 0x67"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo(MensajesGlobales.PETICION_ILEGIBLE);
    }

    @Test
    @DisplayName("un /{id} con letras es 400, no 500")
    void idQueNoEsNumero() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarPeticionIlegible(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("un parametro obligatorio que falta es 400, no 500")
    void parametroQueFalta() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarPeticionIlegible(
                new MissingServletRequestParameterException("grupo", "String"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("el detalle tecnico NO viaja al cliente")
    void sinDetalleTecnico() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarPeticionIlegible(
                new HttpMessageNotReadableException(
                        "guardian.dto.admin.PersonaRequest[\"documento\"]"));

        // El mensaje de la excepcion nombra clases y campos internos: devolverlo
        // le regala al atacante la estructura de los DTO.
        assertThat(respuesta.getBody().getMensaje()).isEqualTo(MensajesGlobales.PETICION_ILEGIBLE);
        assertThat(respuesta.getBody().getDetalles()).isNull();
    }

    @Test
    @DisplayName("lo que de verdad no se controlo sigue siendo 500")
    void loInesperadoSigueSiendo500() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.manejarInesperado(new IllegalStateException("se cayo la base"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo(MensajesGlobales.ERROR_INESPERADO);
    }
}
