package com.manuhd.app.contratos.exception;

/**
 * Excepción lanzada cuando se intenta una transición de estado inválida.
 * Resultado: HTTP 422 Unprocessable Entity
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final String currentState;
    private final String targetState;
    private final String entityType;

    public InvalidStateTransitionException(String entityType, String currentState, String targetState) {
        super(String.format("Transición de estado inválida para %s: no se puede pasar de '%s' a '%s'",
                entityType, currentState, targetState));
        this.entityType = entityType;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public InvalidStateTransitionException(String message) {
        super(message);
        this.entityType = null;
        this.currentState = null;
        this.targetState = null;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }

    public String getEntityType() {
        return entityType;
    }
}
