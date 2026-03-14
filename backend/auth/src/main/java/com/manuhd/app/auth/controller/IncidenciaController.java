package com.manuhd.app.auth.controller;

import com.manuhd.app.auth.dto.*;
import com.manuhd.app.auth.service.IncidenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    public IncidenciaController(IncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @GetMapping
    ResponseEntity<List<IncidenciaResumenDto>> listar() {
        return ResponseEntity.ok(incidenciaService.listar());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @GetMapping("/{id}")
    ResponseEntity<IncidenciaDto> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(incidenciaService.obtener(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    ResponseEntity<IncidenciaDto> crear(@RequestBody CreateIncidenciaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(req));
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @PutMapping("/{id}/estado")
    ResponseEntity<IncidenciaDto> cambiarEstado(@PathVariable Long id,
                                                @RequestBody CambiarEstadoRequest req) {
        try {
            return ResponseEntity.ok(incidenciaService.cambiarEstado(id, req));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/comentarios")
    ResponseEntity<ComentarioDto> addComentario(@PathVariable Long id,
                                                @RequestBody AddComentarioRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(incidenciaService.addComentario(id, req));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/{id}/comentarios/{comentarioId}")
    ResponseEntity<Void> eliminarComentario(@PathVariable Long id,
                                            @PathVariable Long comentarioId) {
        try {
            incidenciaService.eliminarComentario(id, comentarioId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            incidenciaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
