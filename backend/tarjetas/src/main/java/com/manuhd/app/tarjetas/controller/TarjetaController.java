package com.manuhd.app.tarjetas.controller;

import com.manuhd.app.tarjetas.model.Tarjeta;
import com.manuhd.app.tarjetas.service.TarjetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarjetas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class TarjetaController {

    private final TarjetaService tarjetaService;

    @GetMapping
    public ResponseEntity<List<Tarjeta>> getAllTarjetas() {
        return ResponseEntity.ok(tarjetaService.findAll());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<Tarjeta>> getAllTarjetasActivas() {
        return ResponseEntity.ok(tarjetaService.findAllActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tarjeta> getTarjetaById(@PathVariable Long id) {
        return ResponseEntity.ok(tarjetaService.findById(id));
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<Tarjeta>> getTarjetasBySocioId(@PathVariable Long socioId) {
        return ResponseEntity.ok(tarjetaService.findBySocioId(socioId));
    }

    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<Tarjeta>> getTarjetasByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(tarjetaService.findByPetroleraId(petroleraId));
    }

    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<List<Tarjeta>> getTarjetasByMatricula(@PathVariable String matricula) {
        return ResponseEntity.ok(tarjetaService.findByMatricula(matricula));
    }

    @PostMapping
    public ResponseEntity<Tarjeta> createTarjeta(@Valid @RequestBody Tarjeta tarjeta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarjetaService.create(tarjeta));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tarjeta> updateTarjeta(@PathVariable Long id, @Valid @RequestBody Tarjeta tarjeta) {
        return ResponseEntity.ok(tarjetaService.update(id, tarjeta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarjeta(@PathVariable Long id) {
        tarjetaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
