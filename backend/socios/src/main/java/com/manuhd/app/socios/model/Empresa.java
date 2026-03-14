package com.manuhd.app.socios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id", nullable = false)
    @JsonIgnoreProperties("empresas")
    private Socio socio;
    
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String cif;
    
    @Size(max = 300)
    @Column(length = 300)
    private String direccion;
    
    @Size(max = 100)
    @Column(length = 100)
    private String poblacion;
    
    @Size(max = 100)
    @Column(length = 100)
    private String provincia;
    
    @Size(max = 10)
    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;
    
    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;
    
    @Size(max = 20)
    @Column(length = 20)
    private String telefono;
    
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;
    
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
