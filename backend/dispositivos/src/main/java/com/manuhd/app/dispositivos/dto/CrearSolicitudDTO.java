package com.manuhd.app.dispositivos.dto;

import com.manuhd.app.dispositivos.model.TipoSolicitud;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    // La columna de la solicitud y la del dispositivo son de 20: sin este limite una
    // matricula mas larga no falla hasta el flush, y sale como error interno en vez de
    // como un aviso al operador.
    @Size(max = 20, message = "La matricula no puede exceder 20 caracteres")
    private String matricula;

    @Size(max = 20, message = "La matricula destino no puede exceder 20 caracteres")
    private String matriculaDestino;

    private BigDecimal monto;

    private String observaciones;

    private Boolean programadoEnvio = false;

    private LocalDate fechaProgramadaEnvio;
}
