package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.model.SolicitudContrato;
import com.manuhd.app.contratos.model.TipoSolicitudContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudContratoDTO {

    private Long id;
    private String numeroSolicitud;
    private Long socioId;
    private Long empresaId;
    private Long tarjetaId;
    private Long petroleraId;
    private Long tipoContratoId;
    private Long tipoSolicitudPetroleraId;
    private Long plantillaId;
    private LocalDateTime fechaHoraSolicitud;
    private String solicitadoPor;
    private Boolean esAutonomo;
    private String observaciones;
    private EstadoSolicitud estado;
    private TipoSolicitudContrato tipoSolicitud;
    private Long contratoId;
    private String subtipoNombre;
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
    private LocalDateTime fechaResolucionPetrolera;
    private String motivoRechazo;
    private String correosEnviados;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static SolicitudContratoDTO fromEntity(SolicitudContrato entity) {
        if (entity == null) return null;

        return new SolicitudContratoDTO(
            entity.getId(),
            entity.getNumeroSolicitud(),
            entity.getSocioId(),
            entity.getEmpresaId(),
            entity.getTarjetaId(),
            entity.getPetroleraId(),
            entity.getTipoContratoId(),
            entity.getTipoSolicitudPetroleraId(),
            entity.getPlantillaId(),
            entity.getFechaHoraSolicitud(),
            entity.getSolicitadoPor(),
            entity.getEsAutonomo(),
            entity.getObservaciones(),
            entity.getEstado(),
            entity.getTipoSolicitud(),
            entity.getContratoId(),
            entity.getSubtipoNombre(),
            entity.getRutaPdfEditable(),
            entity.getNombrePdfEditable(),
            entity.getRutaPdfEnviado(),
            entity.getNombrePdfEnviado(),
            entity.getRutaPdfFirmado(),
            entity.getNombrePdfFirmado(),
            entity.getRutaPdfFinal(),
            entity.getNombrePdfFinal(),
            entity.getFechaEnvioSocio(),
            entity.getFechaRecepcionFirmado(),
            entity.getFechaEnvioPetrolera(),
            entity.getFechaResolucionPetrolera(),
            entity.getMotivoRechazo(),
            entity.getCorreosEnviados(),
            entity.getFechaCreacion(),
            entity.getFechaActualizacion()
        );
    }
}
