package com.manuhd.app.tarjetas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetroleraDTO {
    private Long id;
    private String nombre;
    private String email;

    /**
     * Si la petrolera trabaja con tarjetas. {@code null} significa "sin restricción
     * configurada" (petroleras dadas de alta antes del flag), y por tanto permitido.
     */
    private Boolean operaTarjetas;

    /** Petrolera sin restricción configurada: el flag se deja sin valor. */
    public PetroleraDTO(Long id, String nombre, String email) {
        this(id, nombre, email, null);
    }
}
