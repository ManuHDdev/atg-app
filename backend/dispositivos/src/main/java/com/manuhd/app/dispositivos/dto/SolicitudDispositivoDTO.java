package com.manuhd.app.dispositivos.dto;

import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDispositivoDTO {

    private Long id;

    private Long socioId;
    private String socioNombre;
    private String socioEmail;

    private Long empresaId;
    private String empresaNombre;
    private String empresaEmail;

    private Long petroleraId;
    private String petroleraNombre;
    private String petroleraEmail;

    private Long dispositivoId;
    private String dispositivoMatricula;

    private TipoSolicitud tipoSolicitud;
    private EstadoSolicitud estado;

    private String matricula;
    private String matriculaDestino;
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
