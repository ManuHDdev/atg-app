package com.manuhd.app.contratos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubirPlantillaDTO {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombrePlantilla;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    @NotNull(message = "El ID del tipo de contrato es obligatorio")
    private Long tipoContratoId;

    private Long tipoSolicitudPetroleraId;

    private Boolean activa = true;
}
