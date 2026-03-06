package com.manuhd.app.auth.dto;

public record UpdateUsuarioRequest(
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        String role,
        String newPassword) {
}
