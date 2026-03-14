package com.manuhd.app.petroleras.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantillas_correo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "petrolera_id", nullable = false)
    @JsonIgnoreProperties({"subsecciones", "plantillasCorreo"})
    private Petrolera petrolera;

    @NotBlank(message = "El tipo de plantilla es obligatorio")
    @Column(name = "tipo_plantilla", nullable = false, length = 50)
    private String tipoPlantilla; // SOLICITUD_CREDITO, AMPLIACION_CREDITO, DEVOLUCION_AVAL

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String asunto;

    @NotBlank(message = "El cuerpo del correo es obligatorio")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String cuerpo;

    @Column(columnDefinition = "TEXT")
    private String variablesDisponibles; // JSON con las variables disponibles para esta plantilla

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
