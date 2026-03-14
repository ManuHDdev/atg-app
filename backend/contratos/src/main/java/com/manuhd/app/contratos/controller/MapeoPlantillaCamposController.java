package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.model.MapeoPlantillaCampos;
import com.manuhd.app.contratos.service.MapeoPlantillaCamposService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mapeos")
@RequiredArgsConstructor
public class MapeoPlantillaCamposController {

    private final MapeoPlantillaCamposService mapeoService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<MapeoPlantillaCampos>> getAllMapeos() {
        return ResponseEntity.ok(mapeoService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<MapeoPlantillaCampos> getMapeoById(@PathVariable Long id) {
        return ResponseEntity.ok(mapeoService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/plantilla/{plantillaId}")
    public ResponseEntity<List<MapeoPlantillaCampos>> getMapeosByPlantillaId(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(mapeoService.findByPlantillaId(plantillaId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/plantilla/{plantillaId}/activos")
    public ResponseEntity<List<MapeoPlantillaCampos>> getMapeosByPlantillaIdActivos(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(mapeoService.findByPlantillaIdActivos(plantillaId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<MapeoPlantillaCampos> createMapeo(@Valid @RequestBody MapeoPlantillaCampos mapeo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapeoService.create(mapeo));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<MapeoPlantillaCampos> updateMapeo(
            @PathVariable Long id,
            @Valid @RequestBody MapeoPlantillaCampos mapeo) {
        return ResponseEntity.ok(mapeoService.update(id, mapeo));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapeo(@PathVariable Long id) {
        mapeoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}/fisico")
    public ResponseEntity<Void> deleteMapeoFisicamente(@PathVariable Long id) {
        mapeoService.deletePhysically(id);
        return ResponseEntity.noContent().build();
    }
}
