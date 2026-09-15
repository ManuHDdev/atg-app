package com.manuhd.app.dispositivos.exception;

/**
 * Error de negocio con un mensaje pensado para el usuario de oficina: falta la plantilla,
 * falta el PDF firmado, etc. El GlobalExceptionHandler ya traduce cualquier RuntimeException
 * a un 400 con ese mensaje, asi que basta con extenderla para que llegue tal cual al frontend.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
