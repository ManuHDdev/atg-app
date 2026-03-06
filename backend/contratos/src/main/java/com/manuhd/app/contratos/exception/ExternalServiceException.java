package com.manuhd.app.contratos.exception;

/**
 * Excepción lanzada cuando falla la comunicación con un servicio externo.
 * Resultado: HTTP 502 Bad Gateway o 503 Service Unavailable
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String operation;

    public ExternalServiceException(String serviceName, String operation, String message) {
        super(String.format("Error en servicio '%s' durante '%s': %s", serviceName, operation, message));
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public ExternalServiceException(String serviceName, String operation, Throwable cause) {
        super(String.format("Error en servicio '%s' durante '%s': %s", serviceName, operation, cause.getMessage()), cause);
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public ExternalServiceException(String message) {
        super(message);
        this.serviceName = null;
        this.operation = null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperation() {
        return operation;
    }
}
