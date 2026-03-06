package com.manuhd.app.tarjetas.dto;

import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTarjetaDTO {
    private Long id;
    private Long socioId;
    private Long petroleraId;
    private String matricula;  // Opcional
    private String numeroContrato;  // Opcional
    private TipoSolicitud tipo;
    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaProcesado;
    private String observaciones;
    private String procesadoPor;
    private String solicitadoPor;  // Persona de oficina
    private Long tarjetaId;  // Para BAJA
    private LocalDate fechaLlegadaEstimada;
    private LocalDateTime fechaEntrega;
    private String correosEnviados;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
