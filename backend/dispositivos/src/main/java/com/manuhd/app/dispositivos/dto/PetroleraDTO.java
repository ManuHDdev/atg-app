package com.manuhd.app.dispositivos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de solo lectura de una petrolera obtenida del microservicio de Petroleras.
 * Solo contiene los campos que este modulo necesita para validar las restricciones
 * del procedimiento de ATG.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetroleraDTO {

    private Long id;
    private String nombre;
    private Boolean activa;

    /** null = sin restriccion: la petrolera opera con dispositivos. */
    private Boolean operaDispositivos;

    /** null = sin restriccion: la petrolera admite solicitudes de credito de dispositivos. */
    private Boolean permiteCreditoDispositivo;
}
