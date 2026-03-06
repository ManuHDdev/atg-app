package com.manuhd.app.tarjetas.dto;

import com.manuhd.app.tarjetas.model.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudDTO {

    @NotNull(message = "El socio es obligatorio")
    private Long socioId;

    @NotNull(message = "La petrolera es obligatoria")
    private Long petroleraId;

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 20, message = "La matrícula no puede exceder 20 caracteres")
    private String matricula;

    @Size(max = 50, message = "El número de contrato no puede exceder 50 caracteres")
    private String numeroContrato;  // Opcional

    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipo;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @Size(max = 100, message = "El campo solicitadoPor no puede exceder 100 caracteres")
    private String solicitadoPor;  // Persona de oficina (requerido en frontend para ALTA)

    private Long tarjetaId;  // ID de tarjeta a dar de baja (para BAJA)

    // Campo para solicitudes de LLEGADA - fecha estimada de entrega/recogida
    private LocalDate fechaLlegadaEstimada;
}
