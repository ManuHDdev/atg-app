package com.manuhd.app.socios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaDTO {
    private Long id;
    private Long socioId;
    private String socioNombre;
    private String nombre;
    private String cif;
    private String direccion;
    private String poblacion;
    private String provincia;
    private String codigoPostal;
    private String email;
    private String telefono;
    private LocalDateTime fechaAlta;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
