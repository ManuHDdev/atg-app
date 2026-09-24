package com.manuhd.app.tarjetas.exception;

import com.manuhd.app.tarjetas.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qué mensaje llega al navegador cuando algo falla.
 *
 * <p>En producción se enseñó el texto en inglés de Spring "Transaction silently rolled back
 * because it has been marked as rollback-only" a una usuaria que solo estaba intentando dar
 * de alta una tarjeta. Los mensajes del framework se quedan en el log; al usuario le llega
 * español.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest peticion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/solicitudes-tarjetas");
        return request;
    }

    @Test
    void unFalloDelFrameworkNoLlegaAlUsuarioConSuTextoEnIngles() {
        UnexpectedRollbackException ex = new UnexpectedRollbackException(
                "Transaction silently rolled back because it has been marked as rollback-only");

        ResponseEntity<ErrorResponse> respuesta = handler.handleRuntimeException(ex, peticion());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getMessage())
                .isEqualTo(GlobalExceptionHandler.MENSAJE_ERROR_INESPERADO)
                .doesNotContain("rollback");
    }

    /** Los errores de negocio sí tienen un mensaje pensado para quien está en la oficina. */
    @Test
    void unErrorDeNegocioSigueLlegandoConSuMensaje() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleRuntimeException(
                new RuntimeException("El motivo del duplicado es obligatorio"), peticion());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getMessage()).isEqualTo("El motivo del duplicado es obligatorio");
    }

    @Test
    void unaBusinessValidationExceptionSigueLlegandoConSuMensaje() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleRuntimeException(
                new BusinessValidationException("La petrolera Repsol no opera con tarjetas"), peticion());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getMessage()).isEqualTo("La petrolera Repsol no opera con tarjetas");
    }
}
