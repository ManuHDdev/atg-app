package com.manuhd.app.tarjetas.dto;

import com.manuhd.app.tarjetas.model.TipoPlantilla;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaTarjetaDTO {
    private Long id;

    @NotNull(message = "El tipo de plantilla es obligatorio")
    private TipoPlantilla tipo;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede exceder 255 caracteres")
    private String asunto;

    @NotBlank(message = "El cuerpo es obligatorio")
    private String cuerpo;

    private String variablesDisponibles;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
