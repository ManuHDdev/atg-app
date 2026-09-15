package com.manuhd.app.petroleras.enums;

/**
 * Módulo de negocio al que pertenece una plantilla de documento.
 *
 * <p>Deliberadamente no incluye CONTRATOS: el circuito de contratos sigue usando
 * las plantillas colgadas de {@code TipoSolicitud}, que pertenecen a la jerarquía
 * TipoContrato → TipoSolicitudPetrolera y no debe reutilizarse aquí.</p>
 */
public enum ModuloDocumento {
    TARJETAS,
    DISPOSITIVOS
}
