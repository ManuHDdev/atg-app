package com.manuhd.app.contratos.model;

public enum EstadoCredito {
    PENDIENTE,              // Creado, esperando envío
    ENVIADO_PETROLERA,      // Enviado a la petrolera
    APROBADO,               // Aprobado por la petrolera
    DENEGADO,               // Denegado por la petrolera
    COMPLETADO              // Proceso completado (socio notificado)
}
