package com.manuhd.app.creditos.model;

public enum EstadoCredito {
    PENDIENTE,              // Creado, esperando envío
    ENVIADO_PETROLERA,      // Enviado a la petrolera
    APROBADO,               // Aprobado por la petrolera
    DENEGADO,               // Denegado por la petrolera
    COMPLETADO_APROBADO,    // Completado - aprobado y socio notificado
    COMPLETADO_DENEGADO     // Completado - denegado y socio notificado
}
