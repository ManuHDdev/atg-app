package com.manuhd.app.dispositivos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El ID del socio es obligatorio")
    @Column(name = "socio_id", nullable = false)
    private Long socioId;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    @Column(name = "petrolera_id", nullable = false)
    private Long petroleraId;

    @NotNull(message = "La matricula es obligatoria")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(name = "solicitud_alta_id")
    private Long solicitudAltaId;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
