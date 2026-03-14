package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.dto.SubseccionPetroleraDTO;
import com.manuhd.app.petroleras.service.SubseccionPetroleraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subsecciones-petrolera")
@RequiredArgsConstructor
public class SubseccionPetroleraController {

    private final SubseccionPetroleraService subseccionService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<SubseccionPetroleraDTO>> getAll() {
        return ResponseEntity.ok(subseccionService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<SubseccionPetroleraDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subseccionService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<SubseccionPetroleraDTO>> getByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(subseccionService.findByPetroleraId(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/activas")
    public ResponseEntity<List<SubseccionPetroleraDTO>> getActivasByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(subseccionService.findActivasByPetroleraId(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<SubseccionPetroleraDTO> create(@Valid @RequestBody SubseccionPetroleraDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subseccionService.create(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<SubseccionPetroleraDTO> update(@PathVariable Long id, @Valid @RequestBody SubseccionPetroleraDTO dto) {
        return ResponseEntity.ok(subseccionService.update(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subseccionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
