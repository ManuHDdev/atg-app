package com.manuhd.app.contratos.model;

public enum EstadoSolicitud {
    BORRADOR("Borrador"),
    ENVIADO_SOCIO("Enviado al Socio"),
    FIRMADO_SOCIO("Firmado por Socio"),
    ENVIADO_PETROLERA("Enviado a Petrolera"),
    ACEPTADA_PETROLERA("Aceptada por Petrolera"),
    RECHAZADA_PETROLERA("Rechazada por Petrolera");

    private final String descripcion;

    EstadoSolicitud(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
