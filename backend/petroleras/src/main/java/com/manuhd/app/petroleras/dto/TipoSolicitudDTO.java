package com.manuhd.app.petroleras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoSolicitudDTO {

    private Long id;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    private String petroleraNombre;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    private Integer orden;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activa;

    private String rutaPlantillaPdf;

    private String nombreArchivoPlantilla;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
