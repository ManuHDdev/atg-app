package com.manuhd.app.tarjetas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Resuelve quién está tramitando la solicitud a partir del token de Keycloak.
 * Nunca se toma del cuerpo de la petición: el cliente no puede falsificar la
 * trazabilidad ("TRAMITADO POR") diciendo que la procesó otra persona.
 */
@Service
public class UsuarioActualService {

    /**
     * Nombre del usuario autenticado (claim {@code preferred_username} del JWT).
     * Si no hay token o el claim viene vacío, cae al nombre de la autenticación
     * y, en último término, a "desconocido".
     */
    public String nombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String preferred = jwt.getToken().getClaimAsString("preferred_username");
            if (preferred != null && !preferred.isBlank()) {
                return preferred;
            }
        }
        return auth != null ? auth.getName() : "desconocido";
    }
}
