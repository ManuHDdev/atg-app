package com.manuhd.app.auth.dto;

public record CreateUsuarioRequest(
        String username,
        String firstName,
        String lastName,
        String email,
        String password,
        boolean enabled,
        String role) {
}
