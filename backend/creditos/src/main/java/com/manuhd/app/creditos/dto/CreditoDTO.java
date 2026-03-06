package com.manuhd.app.creditos.dto;

import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.model.TipoCredito;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoDTO {

    private Long id;

    @NotNull(message = "El ID del socio es obligatorio")
    private Long socioId;

    private String socioNombre;
    private String socioEmail;

    private Long empresaId;
    private String empresaNombre;
    private String empresaEmail;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    private String petroleraNombre;
    private String petroleraEmail;

    @NotNull(message = "El tipo de crédito es obligatorio")
    private TipoCredito tipoCredito;

    @NotNull(message = "El estado es obligatorio")
    private EstadoCredito estado;

    private BigDecimal monto;

    private String observaciones;

    private LocalDateTime fechaEnvioPetrolera;
    private LocalDateTime fechaRespuestaPetrolera;
    private LocalDateTime fechaNotificacionSocio;
    private String respuestaPetrolera;

    private Boolean programadoEnvio;
    private LocalDate fechaProgramadaEnvio;

    private String correosEnviados;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
