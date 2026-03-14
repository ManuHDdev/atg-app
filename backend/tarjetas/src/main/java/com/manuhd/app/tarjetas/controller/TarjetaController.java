package com.manuhd.app.tarjetas.controller;

import com.manuhd.app.tarjetas.model.Tarjeta;
import com.manuhd.app.tarjetas.service.TarjetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
public class TarjetaController {

    private final TarjetaService tarjetaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<Tarjeta>> getAllTarjetas() {
        return ResponseEntity.ok(tarjetaService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/activas")
    public ResponseEntity<List<Tarjeta>> getAllTarjetasActivas() {
        return ResponseEntity.ok(tarjetaService.findAllActivas());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<Tarjeta> getTarjetaById(@PathVariable Long id) {
        return ResponseEntity.ok(tarjetaService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<Tarjeta>> getTarjetasBySocioId(@PathVariable Long socioId) {
        return ResponseEntity.ok(tarjetaService.findBySocioId(socioId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<Tarjeta>> getTarjetasByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(tarjetaService.findByPetroleraId(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<List<Tarjeta>> getTarjetasByMatricula(@PathVariable String matricula) {
        return ResponseEntity.ok(tarjetaService.findByMatricula(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<Tarjeta> createTarjeta(@Valid @RequestBody Tarjeta tarjeta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarjetaService.create(tarjeta));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<Tarjeta> updateTarjeta(@PathVariable Long id, @Valid @RequestBody Tarjeta tarjeta) {
        return ResponseEntity.ok(tarjetaService.update(id, tarjeta));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarjeta(@PathVariable Long id) {
        tarjetaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
