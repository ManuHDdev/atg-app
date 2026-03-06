package com.manuhd.app.contratos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaContratoDTO {
    private Long id;
    private String nombre;
    private Long petroleraId;
    private String tipoContrato;
    private String rutaArchivo;
    private String nombreArchivoOriginal;
    private Boolean activa;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
