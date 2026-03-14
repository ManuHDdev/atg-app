package com.manuhd.app.tarjetas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarjetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarjeta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "socio_id", nullable = false)
    private Long socioId;
    
    @Column(name = "petrolera_id")
    private Long petroleraId;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String matricula;
    
    @Size(max = 50)
    @Column(name = "numero_contrato", length = 50)
    private String numeroContrato;
    
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(nullable = false)
    private Integer cantidad = 1;

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
}
