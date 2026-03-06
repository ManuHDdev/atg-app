package com.manuhd.app.contratos.dto;

import com.manuhd.app.contratos.model.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltroSolicitudesDTO {

    private Long socioId;
    private Long petroleraId;
    private Long tipoContratoId;
    private EstadoSolicitud estado;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "fechaCreacion";
    private String sortDirection = "DESC";
}
