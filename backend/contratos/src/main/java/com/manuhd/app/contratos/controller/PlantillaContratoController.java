package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.model.PlantillaContrato;
import com.manuhd.app.contratos.service.PlantillaContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/plantillas")
@RequiredArgsConstructor
public class PlantillaContratoController {

    private final PlantillaContratoService plantillaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<PlantillaContrato>> getAllPlantillas() {
        return ResponseEntity.ok(plantillaService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/activas")
    public ResponseEntity<List<PlantillaContrato>> getAllPlantillasActivas() {
        return ResponseEntity.ok(plantillaService.findAllActivas());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<PlantillaContrato> getPlantillaById(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<PlantillaContrato>> getPlantillasByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(plantillaService.findByPetroleraId(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/activas")
    public ResponseEntity<List<PlantillaContrato>> getPlantillasByPetroleraIdActivas(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(plantillaService.findByPetroleraIdActivas(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/tipo/{tipoContratoId}")
    public ResponseEntity<List<PlantillaContrato>> getPlantillasByPetroleraAndTipo(
            @PathVariable Long petroleraId,
            @PathVariable Long tipoContratoId) {
        return ResponseEntity.ok(plantillaService.findByPetroleraAndTipo(petroleraId, tipoContratoId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/criterios")
    public ResponseEntity<PlantillaContrato> getPlantillaPorCriterios(
            @RequestParam Long petroleraId,
            @RequestParam Long tipoContratoId,
            @RequestParam(required = false) Long tipoSolicitudPetroleraId) {
        return ResponseEntity.ok(plantillaService.obtenerPlantillaPorCriterios(
            petroleraId, tipoContratoId, tipoSolicitudPetroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}/campos")
    public ResponseEntity<List<String>> getCamposPdf(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(plantillaService.obtenerCamposPdf(id));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<PlantillaContrato> createPlantilla(
            @RequestPart("plantilla") @Valid PlantillaContrato plantilla,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            PlantillaContrato created = plantillaService.create(plantilla, archivo);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<PlantillaContrato> updatePlantilla(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaContrato plantilla) {
        return ResponseEntity.ok(plantillaService.update(id, plantilla));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/archivo")
    public ResponseEntity<PlantillaContrato> updateArchivo(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            PlantillaContrato updated = plantillaService.actualizarArchivo(id, archivo);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlantilla(@PathVariable Long id) {
        plantillaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<Void> activarPlantilla(@PathVariable Long id) {
        plantillaService.activar(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarPlantilla(@PathVariable Long id) {
        plantillaService.desactivar(id);
        return ResponseEntity.ok().build();
    }
}
