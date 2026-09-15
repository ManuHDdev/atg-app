package com.manuhd.app.dispositivos.model;

public enum EstadoSolicitud {
    // Circuito del documento firmado: el impreso se genera desde la plantilla de la
    // petrolera, se envia al socio, el socio lo firma fuera del sistema y se devuelve
    // escaneado. Solo entonces la solicitud se presenta a la petrolera.
    BORRADOR,           // Impreso generado, todavia editable en oficina
    ENVIADO_SOCIO,      // Impreso enviado al socio para que lo firme
    FIRMADO_SOCIO,      // Firma del socio recibida y aceptada en oficina

    /**
     * Estado heredado: era el estado inicial antes del circuito del documento firmado.
     * Las solicitudes nuevas ya no entran aqui (nacen en BORRADOR), pero se conserva
     * porque hay filas en produccion guardadas con este valor y deben seguir siendo
     * legibles y tramitables.
     */
    PENDIENTE,

    ENVIADO_PETROLERA,  // Solicitud presentada a la petrolera, esperando su respuesta
    APROBADO,           // La petrolera ha aprobado la solicitud
    DENEGADO,           // La petrolera ha denegado la solicitud
    COMPLETADO          // Socio notificado del resultado: proceso cerrado
}
