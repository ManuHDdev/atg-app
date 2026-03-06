package com.manuhd.app.petroleras.dto;

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
public class PlantillaCorreoDTO {

    private Long id;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    private String petroleraNombre;

    @NotBlank(message = "El tipo de plantilla es obligatorio")
    private String tipoPlantilla;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255)
    private String asunto;

    @NotBlank(message = "El cuerpo del correo es obligatorio")
    private String cuerpo;

    private String variablesDisponibles;

    private Boolean activa = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
