package com.manuhd.app.socios.model;

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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "socios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Socio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String nombre;
    
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
    
    @Email(message = "Email debe ser válido")
    @Size(max = 150)
    @Column(length = 150)
    private String email;
    
    @Size(max = 20)
    @Column(length = 20)
    private String telefono;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Agrupacion agrupacion;
    
    @NotBlank(message = "El número de socio es obligatorio")
    @Size(max = 50)
    @Column(name = "numero_socio", nullable = false, unique = true, length = 50)
    private String numeroSocio;
    
    @Column(name = "es_autonomo")
    private Boolean esAutonomo = false;
    
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;
    
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "socio", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("socio")
    private List<Empresa> empresas = new ArrayList<>();
}
