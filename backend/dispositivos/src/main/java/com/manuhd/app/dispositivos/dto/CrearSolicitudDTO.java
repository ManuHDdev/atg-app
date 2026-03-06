package com.manuhd.app.dispositivos.dto;

import com.manuhd.app.dispositivos.model.TipoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudDTO {

    @NotNull(message = "El ID del socio es obligatorio")
    private Long socioId;

    private Long empresaId;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipoSolicitud;

    private Long dispositivoId;

    private String matricula;

    private String matriculaDestino;

    private BigDecimal monto;

    private String observaciones;

    private Boolean programadoEnvio = false;

    private LocalDate fechaProgramadaEnvio;
}
