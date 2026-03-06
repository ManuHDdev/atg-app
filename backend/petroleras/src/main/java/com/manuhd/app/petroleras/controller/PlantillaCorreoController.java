package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.dto.PlantillaCorreoDTO;
import com.manuhd.app.petroleras.service.PlantillaCorreoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillas-correo")
@CrossOrigin(origins = "*")
@Slf4j
public class PlantillaCorreoController {

    @Autowired
    private PlantillaCorreoService plantillaCorreoService;

    @GetMapping
    public ResponseEntity<List<PlantillaCorreoDTO>> listarTodas() {
        log.info("GET /api/plantillas-correo - Listar todas las plantillas");
        return ResponseEntity.ok(plantillaCorreoService.listarTodas());
    }

    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<PlantillaCorreoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/plantillas-correo/petrolera/{} - Listar plantillas por petrolera", petroleraId);
        return ResponseEntity.ok(plantillaCorreoService.listarPorPetrolera(petroleraId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaCorreoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/plantillas-correo/{}", id);
        return ResponseEntity.ok(plantillaCorreoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PlantillaCorreoDTO> crear(@Valid @RequestBody PlantillaCorreoDTO dto) {
        log.info("POST /api/plantillas-correo - Crear plantilla");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantillaCorreoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaCorreoDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaCorreoDTO dto) {
        log.info("PUT /api/plantillas-correo/{}", id);
        return ResponseEntity.ok(plantillaCorreoService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PlantillaCorreoDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Boolean> body) {
        log.info("PATCH /api/plantillas-correo/{}/estado", id);
        PlantillaCorreoDTO plantilla = plantillaCorreoService.obtenerPorId(id);
        plantilla.setActiva(body.get("activa"));
        return ResponseEntity.ok(plantillaCorreoService.actualizar(id, plantilla));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/plantillas-correo/{}", id);
        plantillaCorreoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
