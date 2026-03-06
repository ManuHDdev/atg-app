package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.TipoCredito;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearCreditoDTO {

    @NotNull(message = "El ID del socio es obligatorio")
    private Long socioId;

    private Long empresaId;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    @NotNull(message = "El tipo de crédito es obligatorio")
    private TipoCredito tipoCredito;

    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que 0")
    private BigDecimal monto;

    private String observaciones;

    private Boolean programadoEnvio = false;
    private LocalDate fechaProgramadaEnvio;
}
