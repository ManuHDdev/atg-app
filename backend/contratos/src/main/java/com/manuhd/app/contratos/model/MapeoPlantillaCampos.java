package com.manuhd.app.contratos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mapeo_plantilla_campos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapeoPlantillaCampos {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private PlantillaContrato plantilla;
    
    @Size(max = 100)
    @Column(name = "nombre_campo_pdf", length = 100)
    private String nombreCampoPdf;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", length = 20)
    private TipoDato tipoDato;
    
    @Size(max = 100)
    @Column(name = "campo_entidad", length = 100)
    private String campoEntidad;
    
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
