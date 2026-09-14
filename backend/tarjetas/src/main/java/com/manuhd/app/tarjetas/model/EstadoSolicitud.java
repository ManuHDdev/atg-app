package com.manuhd.app.tarjetas.model;

public enum EstadoSolicitud {
    // Circuito del documento firmado: el impreso se genera desde la plantilla de la
    // petrolera, se envía al socio, el socio lo firma fuera del sistema y se devuelve
    // escaneado. Solo entonces la solicitud se presenta a la petrolera.
    BORRADOR,            // Impreso generado, todavía editable en oficina
    ENVIADO_SOCIO,       // Impreso enviado al socio para que lo firme
    FIRMADO_SOCIO,       // Firma del socio recibida y aceptada en oficina
    PENDIENTE,           // Solicitud presentada a la petrolera, esperando su respuesta
    APROBADA,           // La petrolera ha aprobado la solicitud
    RECHAZADA,          // La petrolera ha denegado la solicitud
    TARJETA_LLEGADA,    // La tarjeta física ha llegado y se ha avisado al socio
    COMPLETADA          // Tarjeta entregada al socio: proceso cerrado
}
