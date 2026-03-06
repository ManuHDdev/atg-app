package com.manuhd.app.dispositivos.controller;

import com.manuhd.app.dispositivos.dto.DispositivoDTO;
import com.manuhd.app.dispositivos.service.DispositivoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispositivos")
@Slf4j
public class DispositivoController {

    @Autowired
    private DispositivoService dispositivoService;

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<DispositivoDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/dispositivos/socio/{}", socioId);
        return ResponseEntity.ok(dispositivoService.listarPorSocio(socioId));
    }

    @GetMapping("/socio/{socioId}/activos")
    public ResponseEntity<List<DispositivoDTO>> listarActivosPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/dispositivos/socio/{}/activos", socioId);
        return ResponseEntity.ok(dispositivoService.listarActivosPorSocio(socioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DispositivoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/dispositivos/{}", id);
        return ResponseEntity.ok(dispositivoService.obtenerPorId(id));
    }
}
