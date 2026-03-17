package com.manuhd.app.creditos.dto;

import jakarta.validation.constraints.NotNull;
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
}
