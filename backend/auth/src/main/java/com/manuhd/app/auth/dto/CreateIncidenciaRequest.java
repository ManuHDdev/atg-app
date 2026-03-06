package com.manuhd.app.auth.dto;

import com.manuhd.app.auth.model.Prioridad;
import com.manuhd.app.auth.model.TipoIncidencia;

public record CreateIncidenciaRequest(
        String titulo,
        String descripcion,
        TipoIncidencia tipo,
        Prioridad prioridad) {
}
