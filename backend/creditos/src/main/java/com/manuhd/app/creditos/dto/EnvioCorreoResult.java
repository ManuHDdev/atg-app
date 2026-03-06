package com.manuhd.app.creditos.dto;

import java.time.LocalDateTime;

public class EnvioCorreoResult {
    private boolean exito;
    private String tipoPlantilla;
    private String destinatario;
    private LocalDateTime timestamp;
    private String errorMessage;

    public EnvioCorreoResult(boolean exito, String tipoPlantilla, String destinatario) {
        this.exito = exito;
        this.tipoPlantilla = tipoPlantilla;
        this.destinatario = destinatario;
        this.timestamp = LocalDateTime.now();
    }

    public EnvioCorreoResult(boolean exito, String tipoPlantilla, String destinatario, String errorMessage) {
        this.exito = exito;
        this.tipoPlantilla = tipoPlantilla;
        this.destinatario = destinatario;
        this.timestamp = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public boolean isExito() {
        return exito;
    }

    public String getTipoPlantilla() {
        return tipoPlantilla;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s",
                timestamp,
                tipoPlantilla,
                destinatario,
                exito ? "ENVIADO" : "ERROR - " + errorMessage);
    }
}
