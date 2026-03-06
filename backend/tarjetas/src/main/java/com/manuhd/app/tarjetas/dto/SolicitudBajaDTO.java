package com.manuhd.app.tarjetas.dto;

public class SolicitudBajaDTO extends CrearSolicitudDTO {
    private Long tarjetaId;  // ID de la tarjeta a dar de baja

    public SolicitudBajaDTO() {
        super();
    }

    public Long getTarjetaId() {
        return tarjetaId;
    }

    public void setTarjetaId(Long tarjetaId) {
        this.tarjetaId = tarjetaId;
    }
}
