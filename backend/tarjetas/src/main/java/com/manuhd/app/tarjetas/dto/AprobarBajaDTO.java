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

    private String observaciones;  // Opcional
}
