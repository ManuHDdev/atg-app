package com.manuhd.app.dispositivos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_dispositivo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador funcional de la solicitud ("DIS-2026-00001"). Da nombre al directorio
     * en disco donde viven sus PDFs. Las filas anteriores al circuito del documento firmado
     * lo tienen a null: todo lo que lo lee debe tolerarlo.
     */
    @Size(max = 20, message = "El numero de solicitud no puede exceder 20 caracteres")
    @Column(name = "numero_solicitud", length = 20, unique = true)
    private String numeroSolicitud;

    @NotNull(message = "El ID del socio es obligatorio")
    @Column(name = "socio_id", nullable = false)
    private Long socioId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    @Column(name = "petrolera_id", nullable = false)
    private Long petroleraId;

    @Column(name = "dispositivo_id")
    private Long dispositivoId;

    @NotNull(message = "El tipo de solicitud es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 30)
    private TipoSolicitud tipoSolicitud;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.BORRADOR;

    @Column(length = 20)
    private String matricula;

    @Column(name = "matricula_destino", length = 20)
    private String matriculaDestino;

    /** Importe SOLICITADO a la petrolera (solo para tipo SOLICITUD_CREDITO). */
    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    /**
     * Importe realmente CONCEDIDO por la petrolera (solo para tipo SOLICITUD_CREDITO).
     *
     * Null mientras no haya respuesta, si la solicitud fue denegada o si el tipo no lleva
     * importe. Puede diferir del solicitado.
     */
    @Column(name = "monto_concedido", precision = 10, scale = 2)
    private BigDecimal montoConcedido;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // ---- Circuito del documento firmado ----
    // Un fichero por etapa dentro del directorio de la solicitud: el impreso editable en
    // oficina, el aplanado que se manda al socio, el que devuelve firmado y el que se
    // remite a la petrolera.

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

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "fecha_envio_petrolera")
    private LocalDateTime fechaEnvioPetrolera;

    @Column(name = "fecha_respuesta_petrolera")
    private LocalDateTime fechaRespuestaPetrolera;

    @Column(name = "fecha_notificacion_socio")
    private LocalDateTime fechaNotificacionSocio;

    @Column(name = "respuesta_petrolera", columnDefinition = "TEXT")
    private String respuestaPetrolera;

    @Column(name = "programado_envio")
    private Boolean programadoEnvio = false;

    @Column(name = "fecha_programada_envio")
    private LocalDate fechaProgramadaEnvio;

    @Column(name = "correos_enviados", columnDefinition = "TEXT")
    private String correosEnviados;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
