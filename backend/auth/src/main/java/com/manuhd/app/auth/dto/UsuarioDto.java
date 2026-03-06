package com.manuhd.app.auth.dto;

import java.util.List;

public record UsuarioDto(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        List<String> roles) {
}
