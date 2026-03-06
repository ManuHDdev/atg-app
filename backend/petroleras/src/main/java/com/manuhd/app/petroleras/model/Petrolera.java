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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "petroleras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Petrolera {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
    
    @Column(nullable = false)
    private Boolean activa = true;

    @Email(message = "El email debe ser válido")
    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Column(name = "dias_envio_creditos", length = 100)
    private String diasEnvioCreditos;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "petrolera", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("petrolera")
    private List<SubseccionPetrolera> subsecciones = new ArrayList<>();
}
