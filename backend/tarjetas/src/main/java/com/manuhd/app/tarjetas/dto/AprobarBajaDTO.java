package com.manuhd.app.tarjetas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AprobarBajaDTO {

    @NotNull(message = "La fecha de baja es obligatoria")
    private LocalDate fechaBaja;

    @NotNull(message = "El procesadoPor es obligatorio")
    private String procesadoPor;

    private String observaciones;  // Opcional
}
