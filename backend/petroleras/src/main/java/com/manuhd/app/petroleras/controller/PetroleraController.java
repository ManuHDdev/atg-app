package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.service.PetroleraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/petroleras")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PetroleraController {

    private final PetroleraService petroleraService;

    @GetMapping
    public ResponseEntity<List<Petrolera>> getAllPetroleras() {
        return ResponseEntity.ok(petroleraService.findAll());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<Petrolera>> getAllPetrolerasActivas() {
        return ResponseEntity.ok(petroleraService.findAllActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Petrolera> getPetroleraById(@PathVariable Long id) {
        return ResponseEntity.ok(petroleraService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Petrolera> createPetrolera(@Valid @RequestBody Petrolera petrolera) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petroleraService.create(petrolera));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Petrolera> updatePetrolera(@PathVariable Long id, @Valid @RequestBody Petrolera petrolera) {
        return ResponseEntity.ok(petroleraService.update(id, petrolera));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePetrolera(@PathVariable Long id) {
        petroleraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
