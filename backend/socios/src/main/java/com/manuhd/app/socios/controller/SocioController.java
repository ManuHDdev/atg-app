package com.manuhd.app.socios.controller;

import com.manuhd.app.socios.model.Socio;
import com.manuhd.app.socios.service.SocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/socios")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SocioController {
    
    private final SocioService socioService;
    
    @GetMapping
    public ResponseEntity<List<Socio>> getAllSocios() {
        return ResponseEntity.ok(socioService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Socio> getSocioById(@PathVariable Long id) {
        return ResponseEntity.ok(socioService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<Socio> createSocio(@Valid @RequestBody Socio socio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socioService.create(socio));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Socio> updateSocio(@PathVariable Long id, @Valid @RequestBody Socio socio) {
        return ResponseEntity.ok(socioService.update(id, socio));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSocio(@PathVariable Long id) {
        socioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
