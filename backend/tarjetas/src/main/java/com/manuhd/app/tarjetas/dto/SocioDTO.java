package com.manuhd.app.tarjetas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocioDTO {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String poblacion;
    private String codigoPostal;
    private String provincia;
    private String numeroSocio;
}
