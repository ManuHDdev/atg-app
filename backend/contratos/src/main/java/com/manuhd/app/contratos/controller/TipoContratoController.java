package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.dto.TipoContratoDTO;
import com.manuhd.app.contratos.service.TipoContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-contrato")
@RequiredArgsConstructor
@Slf4j
public class TipoContratoController {

    private final TipoContratoService tipoContratoService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<TipoContratoDTO>> listarTodos() {
        log.info("GET /api/tipos-contrato - Listar todos");
        return ResponseEntity.ok(tipoContratoService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/activos")
    public ResponseEntity<List<TipoContratoDTO>> listarActivos() {
        log.info("GET /api/tipos-contrato/activos - Listar activos");
        return ResponseEntity.ok(tipoContratoService.listarActivos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<TipoContratoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/tipos-contrato/{} - Obtener por ID", id);
        return ResponseEntity.ok(tipoContratoService.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<TipoContratoDTO> obtenerPorCodigo(@PathVariable String codigo) {
        log.info("GET /api/tipos-contrato/codigo/{} - Obtener por código", codigo);
        return ResponseEntity.ok(tipoContratoService.obtenerPorCodigo(codigo));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<TipoContratoDTO> crear(@Valid @RequestBody TipoContratoDTO dto) {
        log.info("POST /api/tipos-contrato - Crear nuevo tipo de contrato");
        TipoContratoDTO creado = tipoContratoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoContratoDTO> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody TipoContratoDTO dto) {
        log.info("PUT /api/tipos-contrato/{} - Actualizar", id);
        return ResponseEntity.ok(tipoContratoService.actualizar(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/tipos-contrato/{} - Eliminar", id);
        tipoContratoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        log.info("PATCH /api/tipos-contrato/{}/activar", id);
        tipoContratoService.activar(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("PATCH /api/tipos-contrato/{}/desactivar", id);
        tipoContratoService.desactivar(id);
        return ResponseEntity.ok().build();
    }
}
