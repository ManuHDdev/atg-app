package com.manuhd.app.contratos.model;

public enum TipoSolicitudContrato {
    NUEVO,              // Nuevo contrato (anterior comportamiento por defecto)
    BAJA,               // Baja de contrato existente
    CAMBIO_CONDICIONES  // Cambio de condiciones de contrato existente
}
