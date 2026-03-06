package com.manuhd.app.tarjetas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaDTO {
    private Long id;
    private Long socioId;
    private Long petroleraId;
    private String matricula;
    private String numeroContrato;
    private LocalDate fechaAlta;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
