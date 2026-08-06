package guardian.exception;

import guardian.constant.MensajesGlobales;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce cualquier excepcion a un {@link ErrorResponse}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GuardianException.class)
    public ResponseEntity<ErrorResponse> manejarDominio(GuardianException ex) {
        log.warn("[error] {} - {}", ex.getEstado().value(), ex.getMessage());
        return ResponseEntity
                .status(ex.getEstado())
                .body(new ErrorResponse(new Date(), ex.getEstado().value(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describirCampo)
                .collect(Collectors.toList());
        log.warn("[error] validacion fallida detalles={}", detalles);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                        "Revisa los datos del formulario.", detalles));
    }

    /**
     * Dos escrituras simultaneas sobre la misma fila (dos porterias con la
     * misma invitacion, dos revocaciones a la vez). El que llega segundo pierde
     * y reintenta; eso es exactamente lo que el bloqueo optimista promete.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> manejarConcurrencia(
            ObjectOptimisticLockingFailureException ex) {
        log.warn("[error] escritura concurrente perdida entidad={}", ex.getPersistentClassName());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(new Date(), HttpStatus.CONFLICT.value(),
                        MensajesGlobales.OPERACION_CRUZADA, null));
    }

    /**
     * La peticion no se pudo leer: cuerpo JSON malformado o mal codificado, un
     * {@code /{id}} con letras, un parametro obligatorio que no vino.
     *
     * <p>Todo esto es culpa de QUIEN LLAMA, y sin este handler caia en el
     * catch-all y salia como 500. Un 500 dice "el servidor se rompio": levanta
     * alertas de infraestructura por algo que no lo es, y deja un stack trace
     * completo en el log por cada peticion basura que llegue —que en un
     * endpoint publico son todas las que quiera mandar un escaner.</p>
     *
     * <p>Se registra la causa en una linea y sin stack: el tipo de excepcion ya
     * dice que fue, y el detalle no se le devuelve al cliente porque describe
     * la estructura interna de los DTO.</p>
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> manejarPeticionIlegible(Exception ex) {
        log.warn("[error] peticion ilegible {}: {}",
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                        MensajesGlobales.PETICION_ILEGIBLE, null));
    }

    /**
     * Ultimo recurso. Se registra el stack completo pero al usuario le llega un
     * mensaje generico: un stack trace en pantalla no le sirve a nadie y le
     * regala al atacante la estructura interna del sistema.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex) {
        log.error("[error] excepcion no controlada", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(new Date(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        MensajesGlobales.ERROR_INESPERADO, null));
    }

    private String describirCampo(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
