package com.manuhd.app.contratos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contrato", uniqueConstraints = {
    @UniqueConstraint(columnNames = "numero_solicitud")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "numero_solicitud", nullable = false, unique = true, length = 50)
    private String numeroSolicitud;

    @NotNull
    @Column(name = "socio_id", nullable = false)
    private Long socioId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "tarjeta_id")
    private Long tarjetaId;

    @NotNull
    @Column(name = "petrolera_id", nullable = false)
    private Long petroleraId;

    @Column(name = "tipo_contrato_id")
    private Long tipoContratoId;  // Opcional - solo para algunos tipos de solicitudes

    @Column(name = "tipo_solicitud_petrolera_id")
    private Long tipoSolicitudPetroleraId;

    @Column(name = "plantilla_id")
    private Long plantillaId;

    @NotNull
    @Column(name = "fecha_hora_solicitud", nullable = false)
    private LocalDateTime fechaHoraSolicitud;

    @Size(max = 100)
    @Column(name = "solicitado_por", length = 100)
    private String solicitadoPor;

    @Column(name = "es_autonomo", nullable = false)
    private Boolean esAutonomo = false;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoSolicitud estado = EstadoSolicitud.BORRADOR;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 50)
    private TipoSolicitudContrato tipoSolicitud = TipoSolicitudContrato.NUEVO;

    @Column(name = "contrato_id")
    private Long contratoId;  // ID del contrato para solicitudes de BAJA o CAMBIO_CONDICIONES

    @Size(max = 200)
    @Column(name = "subtipo_nombre", length = 200)
    private String subtipoNombre; // Subtipo heredado del contrato original (ej: "Precio Lista")

    @Size(max = 500)
    @Column(name = "ruta_pdf_editable", length = 500)
    private String rutaPdfEditable;

    @Size(max = 255)
    @Column(name = "nombre_pdf_editable", length = 255)
    private String nombrePdfEditable;

    @Size(max = 500)
    @Column(name = "ruta_pdf_enviado", length = 500)
    private String rutaPdfEnviado;

    @Size(max = 255)
    @Column(name = "nombre_pdf_enviado", length = 255)
    private String nombrePdfEnviado;

    @Size(max = 500)
    @Column(name = "ruta_pdf_firmado", length = 500)
    private String rutaPdfFirmado;

    @Size(max = 255)
    @Column(name = "nombre_pdf_firmado", length = 255)
    private String nombrePdfFirmado;

    @Size(max = 500)
    @Column(name = "ruta_pdf_final", length = 500)
    private String rutaPdfFinal;

    @Size(max = 255)
    @Column(name = "nombre_pdf_final", length = 255)
    private String nombrePdfFinal;

    @Column(name = "fecha_envio_socio")
    private LocalDateTime fechaEnvioSocio;

    @Column(name = "fecha_recepcion_firmado")
    private LocalDateTime fechaRecepcionFirmado;

    @Column(name = "fecha_envio_petrolera")
    private LocalDateTime fechaEnvioPetrolera;

    @Column(name = "fecha_resolucion_petrolera")
    private LocalDateTime fechaResolucionPetrolera;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "correos_enviados", columnDefinition = "TEXT")
    private String correosEnviados;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
