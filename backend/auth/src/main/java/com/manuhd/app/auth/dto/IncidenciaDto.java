package com.manuhd.app.auth.dto;

import com.manuhd.app.auth.model.*;

import java.time.LocalDateTime;
import java.util.List;

public record IncidenciaDto(
        Long id,
        String titulo,
        String descripcion,
        TipoIncidencia tipo,
        Prioridad prioridad,
        EstadoIncidencia estado,
        String autor,
        String notasDeveloper,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        List<ComentarioDto> comentarios) {

    public static IncidenciaDto from(Incidencia i) {
        return new IncidenciaDto(
                i.getId(), i.getTitulo(), i.getDescripcion(),
                i.getTipo(), i.getPrioridad(), i.getEstado(),
                i.getAutor(), i.getNotasDeveloper(),
                i.getFechaCreacion(), i.getFechaActualizacion(),
                i.getComentarios().stream().map(ComentarioDto::from).toList());
    }
}
