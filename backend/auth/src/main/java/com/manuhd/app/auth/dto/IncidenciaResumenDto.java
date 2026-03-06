package com.manuhd.app.auth.dto;

import com.manuhd.app.auth.model.*;

import java.time.LocalDateTime;

public record IncidenciaResumenDto(
        Long id,
        String titulo,
        TipoIncidencia tipo,
        Prioridad prioridad,
        EstadoIncidencia estado,
        String autor,
        LocalDateTime fechaCreacion,
        int numComentarios) {

    public static IncidenciaResumenDto from(Incidencia i) {
        return new IncidenciaResumenDto(
                i.getId(), i.getTitulo(), i.getTipo(), i.getPrioridad(),
                i.getEstado(), i.getAutor(), i.getFechaCreacion(),
                i.getComentarios().size());
    }
}
