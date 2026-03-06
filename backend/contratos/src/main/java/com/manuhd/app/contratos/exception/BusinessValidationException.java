package com.manuhd.app.contratos.exception;

/**
 * Excepción lanzada cuando falla una validación de regla de negocio.
 * Resultado: HTTP 400 Bad Request
 */
public class BusinessValidationException extends RuntimeException {

    private final String field;
    private final String code;

    public BusinessValidationException(String message) {
        super(message);
        this.field = null;
        this.code = null;
    }

    public BusinessValidationException(String message, String field) {
        super(message);
        this.field = field;
        this.code = null;
    }

    public BusinessValidationException(String message, String field, String code) {
        super(message);
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
