package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.service.PetroleraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/petroleras")
@RequiredArgsConstructor
public class PetroleraController {

    private final PetroleraService petroleraService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<Petrolera>> getAllPetroleras() {
        return ResponseEntity.ok(petroleraService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/activas")
    public ResponseEntity<List<Petrolera>> getAllPetrolerasActivas() {
        return ResponseEntity.ok(petroleraService.findAllActivas());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<Petrolera> getPetroleraById(@PathVariable Long id) {
        return ResponseEntity.ok(petroleraService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<Petrolera> createPetrolera(@Valid @RequestBody Petrolera petrolera) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petroleraService.create(petrolera));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<Petrolera> updatePetrolera(@PathVariable Long id, @Valid @RequestBody Petrolera petrolera) {
        return ResponseEntity.ok(petroleraService.update(id, petrolera));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePetrolera(@PathVariable Long id) {
        petroleraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
