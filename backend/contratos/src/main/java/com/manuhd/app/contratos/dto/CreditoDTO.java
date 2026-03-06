package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.EstadoCredito;
import com.manuhd.app.contratos.model.TipoCredito;
import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que 0")
    private BigDecimal monto;

    private String observaciones;

    private LocalDateTime fechaEnvioPetrolera;
    private LocalDateTime fechaRespuestaPetrolera;
    private LocalDateTime fechaNotificacionSocio;
    private String respuestaPetrolera;

    private Boolean programadoEnvio;
    private LocalDate fechaProgramadaEnvio;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
