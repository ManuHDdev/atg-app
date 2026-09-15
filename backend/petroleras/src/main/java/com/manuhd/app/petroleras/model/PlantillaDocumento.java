package com.manuhd.app.petroleras.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manuhd.app.petroleras.enums.ModuloDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plantilla PDF que la ATG usa para generar el documento que el socio debe firmar,
 * independiente por módulo (tarjetas, dispositivos) y por petrolera.
 *
 * <p>No hay restricción UNIQUE a nivel de base de datos sobre
 * (petrolera_id, modulo, tipo_solicitud): con borrado lógico las filas eliminadas
 * permanecen en la tabla y un índice único las seguiría contando, impidiendo volver
 * a configurar una plantilla que se borró. MySQL no soporta índices únicos parciales
 * (filtrados por activo = true), así que la unicidad "entre filas activas" se valida
 * en {@code PlantillaDocumentoService}.</p>
 */
@Entity
@Table(name = "plantillas_documento",
       indexes = @Index(name = "idx_plantilla_documento_clave",
                        columnList = "petrolera_id, modulo, tipo_solicitud"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "petrolera_id", nullable = false)
    private Long petroleraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuloDocumento modulo;

    /**
     * Nombre de la constante del enum propio del módulo consumidor
     * (p. ej. "ALTA" en tarjetas o "CAMBIO_MATRICULA" en dispositivos).
     * Se guarda como String a propósito: petroleras no debe depender de los enums
     * de los microservicios de tarjetas ni de dispositivos.
     */
    @Column(name = "tipo_solicitud", nullable = false, length = 40)
    private String tipoSolicitud;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;

    /**
     * Estado funcional: si la plantilla está habilitada para usarse.
     * Lo alterna el usuario desde PATCH /{id}/estado. Una plantilla inactiva
     * sigue existiendo y ocupando la clave (petrolera, módulo, tipo de solicitud).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /**
     * Borrado lógico (convención global del proyecto). {@code false} + {@code deletedAt}
     * significa que la fila ya no existe para el negocio y libera la clave
     * (petrolera, módulo, tipo de solicitud) para una plantilla nueva.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @JsonIgnore
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (activa == null) activa = true;
        if (activo == null) activo = true;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
