package com.manuhd.app.petroleras.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetroleraDTO {
    private Long id;
    private String nombre;
    private Boolean activa;
    // null = sin restriccion (la petrolera opera en ese modulo)
    private Boolean operaTarjetas;
    private Boolean operaContratos;
    private Boolean operaCreditos;
    private Boolean operaDispositivos;
    private Boolean permiteCreditoDispositivo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
