package guardian.exception;

import guardian.constant.MensajesGlobales;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
