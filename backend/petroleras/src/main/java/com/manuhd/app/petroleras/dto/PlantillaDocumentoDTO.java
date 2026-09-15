package com.manuhd.app.petroleras.dto;

import com.manuhd.app.petroleras.enums.ModuloDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaDocumentoDTO {

    private Long id;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    private String petroleraNombre;

    @NotNull(message = "El módulo es obligatorio")
    private ModuloDocumento modulo;

    @NotBlank(message = "El tipo de solicitud es obligatorio")
    @Size(max = 40, message = "El tipo de solicitud no puede superar los 40 caracteres")
    private String tipoSolicitud;

    private String nombreArchivo;

    private String rutaArchivo;

    private Boolean activa;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
