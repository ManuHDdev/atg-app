package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.TipoSolicitudContrato;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudDTO {

    @NotNull(message = "El ID del socio es obligatorio")
    private Long socioId;

    private Long empresaId;

    private Long tarjetaId;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    private Long tipoContratoId;  // Opcional - solo para algunos tipos de solicitudes

    private Long tipoSolicitudPetroleraId;

    @Size(max = 100, message = "El campo solicitado por no puede exceder 100 caracteres")
    private String solicitadoPor;

    @NotNull(message = "Debe indicar si es autónomo")
    private Boolean esAutonomo;

    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitudContrato tipoSolicitud = TipoSolicitudContrato.NUEVO;

    private Long contratoId;  // Para solicitudes de BAJA o CAMBIO_CONDICIONES

    private String observaciones;
}
