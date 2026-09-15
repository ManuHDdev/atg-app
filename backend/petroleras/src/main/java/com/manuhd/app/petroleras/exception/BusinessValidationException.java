package com.manuhd.app.petroleras.exception;

/**
 * Error de regla de negocio. El GlobalExceptionHandler lo traduce a 400 Bad Request
 * a través del manejador de RuntimeException.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
