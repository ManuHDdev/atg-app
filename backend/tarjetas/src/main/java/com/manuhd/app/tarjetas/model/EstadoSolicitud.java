package com.manuhd.app.tarjetas.model;

public enum EstadoSolicitud {
    PENDIENTE,           // Solicitud creada, esperando aprobación
    APROBADA,           // Aprobada por petrolera
    RECHAZADA,          // Rechazada por petrolera
    TARJETA_LLEGADA,    // La tarjeta física ha llegado
    ENTREGADA,          // Tarjeta entregada al socio
    COMPLETADA          // Proceso finalizado completamente
}
