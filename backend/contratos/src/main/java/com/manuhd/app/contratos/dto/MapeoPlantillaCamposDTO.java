package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.TipoDato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapeoPlantillaCamposDTO {
    private Long id;
    private Long plantillaId;
    private String nombreCampoPdf;
    private TipoDato tipoDato;
    private String campoEntidad;
    private Boolean activo;
}
