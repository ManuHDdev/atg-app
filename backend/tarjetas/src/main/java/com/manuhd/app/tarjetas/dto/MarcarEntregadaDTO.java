package com.manuhd.app.tarjetas.dto;

import java.time.LocalDateTime;

public class MarcarEntregadaDTO {
    private LocalDateTime fechaEntrega;
    private String observaciones;

    public MarcarEntregadaDTO() {
    }

    public MarcarEntregadaDTO(LocalDateTime fechaEntrega, String observaciones) {
        this.fechaEntrega = fechaEntrega;
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
