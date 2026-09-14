package com.manuhd.app.tarjetas.model;

/**
 * Motivo por el que el socio pide un duplicado de tarjeta. El impreso de duplicado
 * (DOCUMENTO 7) es el mismo impreso del alta con el motivo escrito a mano, y en la
 * práctica solo se dan estos dos casos.
 */
public enum MotivoDuplicado {

    DETERIORO("Deterioro"),
    EXTRAVIO("Extravío");

    private final String etiqueta;

    MotivoDuplicado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para el socio, tal y como debe aparecer en los correos. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
