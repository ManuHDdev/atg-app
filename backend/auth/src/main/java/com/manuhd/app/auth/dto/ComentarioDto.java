package com.manuhd.app.auth.dto;

import com.manuhd.app.auth.model.Comentario;

import java.time.LocalDateTime;

public record ComentarioDto(
        Long id,
        String texto,
        String autor,
        LocalDateTime fecha) {

    public static ComentarioDto from(Comentario c) {
        return new ComentarioDto(c.getId(), c.getTexto(), c.getAutor(), c.getFecha());
    }
}
