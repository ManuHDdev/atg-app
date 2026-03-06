package com.manuhd.app.socios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocioDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
