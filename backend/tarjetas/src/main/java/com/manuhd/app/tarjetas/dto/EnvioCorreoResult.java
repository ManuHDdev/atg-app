package com.manuhd.app.tarjetas.dto;

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

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getTipoPlantilla() {
        return tipoPlantilla;
    }

    public void setTipoPlantilla(String tipoPlantilla) {
        this.tipoPlantilla = tipoPlantilla;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
