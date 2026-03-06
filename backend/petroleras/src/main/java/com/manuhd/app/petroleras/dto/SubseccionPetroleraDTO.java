package com.manuhd.app.petroleras.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubseccionPetroleraDTO {
    private Long id;
    private Long petroleraId;
    private String petroleraNombre;
    private String nombre;
    private String codigo;
    private Integer orden;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
