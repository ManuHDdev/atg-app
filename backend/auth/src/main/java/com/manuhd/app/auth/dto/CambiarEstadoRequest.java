package com.manuhd.app.auth.dto;

import com.manuhd.app.auth.model.EstadoIncidencia;

public record CambiarEstadoRequest(
        EstadoIncidencia nuevoEstado,
        String notasDeveloper) {
}
