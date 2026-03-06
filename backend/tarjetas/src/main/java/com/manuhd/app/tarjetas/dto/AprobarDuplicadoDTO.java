package com.manuhd.app.tarjetas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AprobarDuplicadoDTO {

    @NotNull(message = "La fecha de respuesta es obligatoria")
    private LocalDate fechaRespuesta;

    @NotNull(message = "El procesadoPor es obligatorio")
    private String procesadoPor;

    private String observaciones;  // Opcional
}
