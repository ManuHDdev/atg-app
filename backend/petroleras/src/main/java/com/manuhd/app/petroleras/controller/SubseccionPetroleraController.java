package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.dto.SubseccionPetroleraDTO;
import com.manuhd.app.petroleras.service.SubseccionPetroleraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subsecciones-petrolera")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SubseccionPetroleraController {

    private final SubseccionPetroleraService subseccionService;

    @GetMapping
    public ResponseEntity<List<SubseccionPetroleraDTO>> getAll() {
        return ResponseEntity.ok(subseccionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubseccionPetroleraDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subseccionService.findById(id));
    }

    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<SubseccionPetroleraDTO>> getByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(subseccionService.findByPetroleraId(petroleraId));
    }

    @GetMapping("/petrolera/{petroleraId}/activas")
    public ResponseEntity<List<SubseccionPetroleraDTO>> getActivasByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(subseccionService.findActivasByPetroleraId(petroleraId));
    }

    @PostMapping
    public ResponseEntity<SubseccionPetroleraDTO> create(@Valid @RequestBody SubseccionPetroleraDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subseccionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubseccionPetroleraDTO> update(@PathVariable Long id, @Valid @RequestBody SubseccionPetroleraDTO dto) {
        return ResponseEntity.ok(subseccionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subseccionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
