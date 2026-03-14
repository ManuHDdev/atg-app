package com.manuhd.app.tarjetas.controller;

import com.manuhd.app.tarjetas.dto.PlantillaTarjetaDTO;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.service.PlantillaTarjetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillas-tarjetas")
@RequiredArgsConstructor
@Slf4j
public class PlantillaTarjetaController {

    private final PlantillaTarjetaService service;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<PlantillaTarjetaDTO>> listarTodas() {
        log.info("GET /api/plantillas-tarjetas - Listar todas las plantillas");
        List<PlantillaTarjetaDTO> plantillas = service.findAll();
        return ResponseEntity.ok(plantillas);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<PlantillaTarjetaDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/plantillas-tarjetas/{} - Obtener plantilla por ID", id);
        PlantillaTarjetaDTO plantilla = service.findById(id);
        return ResponseEntity.ok(plantilla);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<PlantillaTarjetaDTO> obtenerPorTipo(@PathVariable TipoPlantilla tipo) {
        log.info("GET /api/plantillas-tarjetas/tipo/{} - Obtener plantilla por tipo", tipo);
        PlantillaTarjetaDTO plantilla = service.findByTipo(tipo);
        return ResponseEntity.ok(plantilla);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<PlantillaTarjetaDTO> crear(@Valid @RequestBody PlantillaTarjetaDTO dto) {
        log.info("POST /api/plantillas-tarjetas - Crear nueva plantilla de tipo: {}", dto.getTipo());
        PlantillaTarjetaDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<PlantillaTarjetaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaTarjetaDTO dto) {
        log.info("PUT /api/plantillas-tarjetas/{} - Actualizar plantilla", id);
        PlantillaTarjetaDTO updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/plantillas-tarjetas/{} - Eliminar plantilla", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
