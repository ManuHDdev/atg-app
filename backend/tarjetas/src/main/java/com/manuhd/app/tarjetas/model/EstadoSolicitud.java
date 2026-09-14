package com.manuhd.app.tarjetas.model;

public enum EstadoSolicitud {
    PENDIENTE,           // Solicitud presentada a la petrolera, esperando su respuesta
    APROBADA,           // La petrolera ha aprobado la solicitud
    RECHAZADA,          // La petrolera ha denegado la solicitud
    TARJETA_LLEGADA,    // La tarjeta física ha llegado y se ha avisado al socio
    COMPLETADA          // Tarjeta entregada al socio: proceso cerrado
}
