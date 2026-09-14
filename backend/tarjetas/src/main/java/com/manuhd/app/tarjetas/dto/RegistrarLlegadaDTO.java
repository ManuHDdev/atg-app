package com.manuhd.app.tarjetas.dto;

import java.time.LocalDate;

public class RegistrarLlegadaDTO {
    private LocalDate fechaLlegadaEstimada;
    private String numeroContrato;  // Opcional
    private String observaciones;

    public RegistrarLlegadaDTO() {
    }

    public RegistrarLlegadaDTO(LocalDate fechaLlegadaEstimada, String numeroContrato, String observaciones) {
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
        this.numeroContrato = numeroContrato;
        this.observaciones = observaciones;
    }

    public LocalDate getFechaLlegadaEstimada() {
        return fechaLlegadaEstimada;
    }

    public void setFechaLlegadaEstimada(LocalDate fechaLlegadaEstimada) {
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
    }

    public String getNumeroContrato() {
        return numeroContrato;
    }

    public void setNumeroContrato(String numeroContrato) {
        this.numeroContrato = numeroContrato;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
