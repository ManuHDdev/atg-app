package com.manuhd.app.creditos.dto;

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
     * Importe concedido por la petrolera. Obligatorio y mayor que 0 cuando se aprueba
     * (salvo devolucion de aval, que no lleva importe). Se ignora al denegar.
     */
    private BigDecimal montoConcedido;
}
