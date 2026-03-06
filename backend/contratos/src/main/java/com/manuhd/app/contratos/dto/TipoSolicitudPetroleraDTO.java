package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.TipoSolicitudPetrolera;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoSolicitudPetroleraDTO {

    private Long id;

    @NotNull(message = "El ID de la petrolera es obligatorio")
    private Long petroleraId;

    @NotNull(message = "El ID del tipo de contrato es obligatorio")
    private Long tipoContratoId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    private Integer orden;

    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static TipoSolicitudPetroleraDTO fromEntity(TipoSolicitudPetrolera entity) {
        if (entity == null) return null;

        return new TipoSolicitudPetroleraDTO(
            entity.getId(),
            entity.getPetroleraId(),
            entity.getTipoContratoId(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.getOrden(),
            entity.getActivo(),
            entity.getFechaCreacion(),
            entity.getFechaActualizacion()
        );
    }

    public TipoSolicitudPetrolera toEntity() {
        TipoSolicitudPetrolera entity = new TipoSolicitudPetrolera();
        entity.setId(this.id);
        entity.setPetroleraId(this.petroleraId);
        entity.setTipoContratoId(this.tipoContratoId);
        entity.setNombre(this.nombre);
        entity.setDescripcion(this.descripcion);
        entity.setOrden(this.orden != null ? this.orden : 0);
        entity.setActivo(this.activo != null ? this.activo : true);
        return entity;
    }
}
