package com.manuhd.app.tarjetas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "plantillas_tarjetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El tipo de plantilla es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private TipoPlantilla tipo;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede exceder 255 caracteres")
    @Column(nullable = false, length = 255)
    private String asunto;

    @NotBlank(message = "El cuerpo es obligatorio")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(name = "variables_disponibles", columnDefinition = "TEXT")
    private String variablesDisponibles;

    @Column(nullable = false)
    private Boolean activa = true;

    @JsonIgnore
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (activa == null) {
            activa = true;
        }
    }
}
