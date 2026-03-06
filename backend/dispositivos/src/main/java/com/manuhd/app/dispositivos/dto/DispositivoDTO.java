package com.manuhd.app.dispositivos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoDTO {

    private Long id;
    private Long socioId;
    private String socioNombre;
    private Long petroleraId;
    private String petroleraNombre;
    private String matricula;
    private Boolean activo;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private LocalDateTime createdAt;
}
