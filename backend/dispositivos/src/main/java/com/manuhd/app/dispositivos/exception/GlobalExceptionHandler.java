package com.manuhd.app.dispositivos.exception;

import com.manuhd.app.dispositivos.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Lo que se le enseña al usuario cuando falla algo que no es un error de negocio. El
     * detalle técnico queda en el log del servidor, que es donde sirve de algo.
     */
    static final String MENSAJE_ERROR_INESPERADO =
            "No se ha podido completar la operación. Vuelva a intentarlo y, si el problema persiste, "
            + "avise al equipo técnico indicando la hora del intento.";

    /**
     * Distingue los errores de negocio de la aplicación de los del framework.
     *
     * <p>Los mensajes de la aplicación están escritos en español y para el usuario, así que se
     * devuelven tal cual. Los del framework no: un {@code UnexpectedRollbackException}, por
     * ejemplo, llega al navegador como "Transaction silently rolled back because it has been
     * marked as rollback-only", que al usuario no le dice nada y destapa detalles internos.
     */
    private static boolean mensajeEscritoParaElUsuario(RuntimeException ex) {
        Class<?> clase = ex.getClass();
        return clase == RuntimeException.class || clase.getName().startsWith("com.manuhd.app.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        if (!mensajeEscritoParaElUsuario(ex)) {
            // Se registra la excepción completa, con su causa y su traza, antes de responder
            // con un mensaje genérico: el diagnóstico no se pierde, solo deja de viajar.
            log.error("Error inesperado atendiendo {}: {}", request.getRequestURI(), ex.getMessage(), ex);

            ErrorResponse errorInesperado = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                MENSAJE_ERROR_INESPERADO,
                request.getRequestURI()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorInesperado);
        }

        log.error("RuntimeException: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Error de validación en los datos enviados",
            request.getRequestURI(),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Ha ocurrido un error interno en el servidor",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
