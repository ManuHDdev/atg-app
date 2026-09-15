package com.manuhd.app.tarjetas.dto;

import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.MotivoDuplicado;
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
    private String numeroSolicitud;  // "TAR-2026-00001"; null en solicitudes anteriores al circuito de firma
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
    private MotivoDuplicado motivoDuplicado;  // Solo para DUPLICADO
    private LocalDate fechaLlegadaEstimada;
    private LocalDateTime fechaEntrega;
    private String correosEnviados;

    // Circuito del documento firmado
    private String rutaPdfEditable;
    private String nombrePdfEditable;
    private String rutaPdfEnviado;
    private String nombrePdfEnviado;
    private String rutaPdfFirmado;
    private String nombrePdfFirmado;
    private String rutaPdfFinal;
    private String nombrePdfFinal;
    private LocalDateTime fechaEnvioSocio;
    private LocalDateTime fechaRecepcionFirmado;
    private LocalDateTime fechaEnvioPetrolera;
    private String motivoRechazo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
