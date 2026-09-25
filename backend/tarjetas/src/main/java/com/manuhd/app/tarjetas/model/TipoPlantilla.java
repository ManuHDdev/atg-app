package com.manuhd.app.tarjetas.model;

public enum TipoPlantilla {
    LLEGADA_MADRID,
    LLEGADA_FUERA,
    ALTA_SOCIO,
    /**
     * @deprecated Ningún flujo envía ya este correo: la petrolera se entera del alta en
     * enviarAPetrolera(), con el documento ya firmado por el socio (DOCUMENTO_PETROLERA).
     * La constante se mantiene porque la columna `tipo` de `plantillas_tarjetas` es un ENUM
     * de MySQL y pueden existir filas guardadas con este valor: eliminarla rompería su
     * lectura. La pantalla de plantillas ya no lo ofrece.
     */
    @Deprecated
    ALTA_PETROLERA,
    ALTA_APROBADA,
    ALTA_RECHAZADA,
    BAJA_SOCIO,
    BAJA_CONFIRMADA,
    DUPLICADO_SOCIO,
    DUPLICADO_CONFIRMADA,
    /**
     * @deprecated Ningún flujo envía ya este correo. Se mantiene por el mismo motivo que
     * ALTA_PETROLERA: pueden existir filas de `plantillas_tarjetas` con este valor.
     * La pantalla de plantillas ya no lo ofrece.
     */
    @Deprecated
    DUPLICADO_PETROLERA,
    // Circuito del documento firmado: el impreso que se manda al socio para su firma y
    // el envío a la petrolera con ese mismo impreso ya firmado.
    DOCUMENTO_SOCIO,
    DOCUMENTO_PETROLERA
}
