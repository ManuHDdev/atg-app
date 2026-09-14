package com.manuhd.app.dispositivos.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponderPetroleraDTO {

    @NotNull(message = "El campo 'aprobado' es obligatorio")
    private Boolean aprobado;

    private String respuesta;

    /**
     * Importe concedido por la petrolera. Obligatorio y mayor que 0 cuando se aprueba una
     * solicitud de tipo SOLICITUD_CREDITO. Se ignora en el resto de casos.
     */
    private BigDecimal montoConcedido;
}
