package com.manuhd.app.contratos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos_socio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoSocio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "socio_id", nullable = false)
    private Long socioId;
    
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "tarjeta_id")
    private Long tarjetaId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id")
    private PlantillaContrato plantilla;

    @Column(name = "petrolera_id")
    private Long petroleraId;

    @Column(name = "subseccion_petrolera_id")
    private Long subseccionPetroleraId;

    @Size(max = 100)
    @Column(name = "tipo_contrato", length = 100)
    private String tipoContrato;

    @Size(max = 200)
    @Column(name = "subtipo_contrato", length = 200)
    private String subtipoContrato; // Nombre del tipo de solicitud petrolera (ej: "Precio Lista", "Via T")

    @Size(max = 20)
    @Column(name = "tipo_solicitante", length = 20)
    private String tipoSolicitante; // "AUTONOMO" o "EMPRESA"

    @Column(name = "fecha_hora_solicitud")
    private LocalDateTime fechaHoraSolicitud;

    @Size(max = 200)
    @Column(name = "solicitado_por", length = 200)
    private String solicitadoPor; // Usuario que creó la solicitud
    
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoContrato estado = EstadoContrato.BORRADOR;

    @Column(nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "fecha_vigencia_desde")
    private LocalDate fechaVigenciaDesde;

    @Column(name = "fecha_vigencia_hasta")
    private LocalDate fechaVigenciaHasta;

    @Size(max = 500)
    @Column(name = "ruta_borrador", length = 500)
    private String rutaBorrador;
    
    @Size(max = 500)
    @Column(name = "ruta_enviado", length = 500)
    private String rutaEnviado;
    
    @Size(max = 500)
    @Column(name = "ruta_firmado", length = 500)
    private String rutaFirmado;
    
    @Size(max = 500)
    @Column(name = "ruta_final", length = 500)
    private String rutaFinal;
    
    @Column(name = "fecha_envio_socio")
    private LocalDateTime fechaEnvioSocio;
    
    @Column(name = "fecha_recepcion_firmado")
    private LocalDateTime fechaRecepcionFirmado;
    
    @Column(name = "fecha_envio_petrolera")
    private LocalDateTime fechaEnvioPetrolera;
    
    @Column(name = "solicitud_id")
    private Long solicitudId; // ID de la SolicitudContrato que originó este contrato

    @Column(columnDefinition = "TEXT")
    private String observaciones;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Exponer plantillaId como campo simple para serialización JSON
    public Long getPlantillaId() {
        return plantilla != null ? plantilla.getId() : null;
    }
}
