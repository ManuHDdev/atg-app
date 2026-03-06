package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.TipoContrato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoContratoDTO {

    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static TipoContratoDTO fromEntity(TipoContrato entity) {
        if (entity == null) return null;

        return new TipoContratoDTO(
            entity.getId(),
            entity.getCodigo(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.getActivo(),
            entity.getFechaCreacion(),
            entity.getFechaActualizacion()
        );
    }

    public TipoContrato toEntity() {
        TipoContrato entity = new TipoContrato();
        entity.setId(this.id);
        entity.setCodigo(this.codigo);
        entity.setNombre(this.nombre);
        entity.setDescripcion(this.descripcion);
        entity.setActivo(this.activo != null ? this.activo : true);
        return entity;
    }
}
