package com.manuhd.app.tarjetas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_tarjetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El socio es obligatorio")
    @Column(name = "socio_id", nullable = false)
    private Long socioId;

    @NotNull(message = "La petrolera es obligatoria")
    @Column(name = "petrolera_id", nullable = false)
    private Long petroleraId;

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 20, message = "La matrícula no puede exceder 20 caracteres")
    @Column(length = 20, nullable = false)
    private String matricula;

    @Size(max = 50, message = "El número de contrato no puede exceder 50 caracteres")
    @Column(name = "numero_contrato", length = 50)
    private String numeroContrato;  // Opcional

    @NotNull(message = "El tipo de solicitud es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSolicitud tipo;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;

    @Column(name = "fecha_llegada_estimada")
    private LocalDate fechaLlegadaEstimada;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "correos_enviados", columnDefinition = "TEXT")
    private String correosEnviados;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    @Column(length = 500)
    private String observaciones;

    @Size(max = 100, message = "El campo procesadoPor no puede exceder 100 caracteres")
    @Column(name = "procesado_por", length = 100)
    private String procesadoPor;

    @Size(max = 100, message = "El campo solicitadoPor no puede exceder 100 caracteres")
    @Column(name = "solicitado_por", length = 100)
    private String solicitadoPor;  // Persona de oficina que solicita (para ALTA)

    @Column(name = "tarjeta_id")
    private Long tarjetaId;  // ID de la tarjeta a dar de baja (para BAJA)

    // Solo tiene sentido en un DUPLICADO: por qué se pide la tarjeta nueva. En el resto
    // de tipos queda a null. Se valida como obligatorio en el servicio, no aquí, porque
    // la obligatoriedad depende del tipo de solicitud.
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_duplicado", length = 20)
    private MotivoDuplicado motivoDuplicado;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoSolicitud.PENDIENTE;
        }
    }
}
