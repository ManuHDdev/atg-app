package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.EstadoContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoSocioDTO {
    private Long id;
    private Long socioId;
    private Long empresaId;
    private Long tarjetaId;
    private Long plantillaId;
    private Long petroleraId;
    private Long subseccionPetroleraId;
    private String tipoContrato;
    private String tipoSolicitante;
    private LocalDateTime fechaHoraSolicitud;
    private String solicitadoPor;
    private EstadoContrato estado;
    private String rutaBorrador;
    private String rutaEnviado;
    private String rutaFirmado;
    private String rutaFinal;
    private LocalDateTime fechaEnvioSocio;
    private LocalDateTime fechaRecepcionFirmado;
    private LocalDateTime fechaEnvioPetrolera;
    private String observaciones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
