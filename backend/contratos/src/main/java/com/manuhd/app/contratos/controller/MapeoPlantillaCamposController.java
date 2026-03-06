package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.model.MapeoPlantillaCampos;
import com.manuhd.app.contratos.service.MapeoPlantillaCamposService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mapeos")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class MapeoPlantillaCamposController {

    private final MapeoPlantillaCamposService mapeoService;

    @GetMapping
    public ResponseEntity<List<MapeoPlantillaCampos>> getAllMapeos() {
        return ResponseEntity.ok(mapeoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MapeoPlantillaCampos> getMapeoById(@PathVariable Long id) {
        return ResponseEntity.ok(mapeoService.findById(id));
    }

    @GetMapping("/plantilla/{plantillaId}")
    public ResponseEntity<List<MapeoPlantillaCampos>> getMapeosByPlantillaId(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(mapeoService.findByPlantillaId(plantillaId));
    }

    @GetMapping("/plantilla/{plantillaId}/activos")
    public ResponseEntity<List<MapeoPlantillaCampos>> getMapeosByPlantillaIdActivos(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(mapeoService.findByPlantillaIdActivos(plantillaId));
    }

    @PostMapping
    public ResponseEntity<MapeoPlantillaCampos> createMapeo(@Valid @RequestBody MapeoPlantillaCampos mapeo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapeoService.create(mapeo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MapeoPlantillaCampos> updateMapeo(
            @PathVariable Long id,
            @Valid @RequestBody MapeoPlantillaCampos mapeo) {
        return ResponseEntity.ok(mapeoService.update(id, mapeo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapeo(@PathVariable Long id) {
        mapeoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/fisico")
    public ResponseEntity<Void> deleteMapeoFisicamente(@PathVariable Long id) {
        mapeoService.deletePhysically(id);
        return ResponseEntity.noContent().build();
    }
}
