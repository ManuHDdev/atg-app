package com.manuhd.app.petroleras.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetroleraDTO {
    private Long id;
    private String nombre;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
