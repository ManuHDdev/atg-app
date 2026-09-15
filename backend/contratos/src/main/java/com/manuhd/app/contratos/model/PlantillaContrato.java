package com.manuhd.app.contratos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantillas_contrato")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaContrato {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "nombre_plantilla", nullable = false, length = 200)
    private String nombrePlantilla;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion;
    
    @Column(name = "petrolera_id")
    private Long petroleraId;

    @Column(name = "tipo_contrato_id")
    private Long tipoContratoId;

    @Column(name = "tipo_solicitud_petrolera_id")
    private Long tipoSolicitudPetroleraId;
    
    @Size(max = 500)
    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;
    
    @Size(max = 255)
    @Column(name = "nombre_archivo_original", length = 255)
    private String nombreArchivoOriginal;
    
    @Column(nullable = false)
    private Boolean activa = true;
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
