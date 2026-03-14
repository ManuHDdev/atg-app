package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.dto.TipoSolicitudPetroleraDTO;
import com.manuhd.app.contratos.service.TipoSolicitudPetroleraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-solicitud-petrolera")
@RequiredArgsConstructor
@Slf4j
public class TipoSolicitudPetroleraController {

    private final TipoSolicitudPetroleraService tipoSolicitudPetroleraService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<TipoSolicitudPetroleraDTO>> listarTodos() {
        log.info("GET /api/tipos-solicitud-petrolera - Listar todos");
        return ResponseEntity.ok(tipoSolicitudPetroleraService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<TipoSolicitudPetroleraDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/tipos-solicitud-petrolera/{} - Obtener por ID", id);
        return ResponseEntity.ok(tipoSolicitudPetroleraService.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<TipoSolicitudPetroleraDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/tipos-solicitud-petrolera/petrolera/{} - Listar por petrolera", petroleraId);
        return ResponseEntity.ok(tipoSolicitudPetroleraService.listarPorPetrolera(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/tipo/{tipoContratoId}")
    public ResponseEntity<List<TipoSolicitudPetroleraDTO>> listarPorPetroleraYTipo(
            @PathVariable Long petroleraId,
            @PathVariable Long tipoContratoId) {
        log.info("GET /api/tipos-solicitud-petrolera/petrolera/{}/tipo/{} - Listar por petrolera y tipo",
            petroleraId, tipoContratoId);
        return ResponseEntity.ok(tipoSolicitudPetroleraService.listarPorPetroleraYTipo(petroleraId, tipoContratoId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/tipo/{tipoContratoId}/activas")
    public ResponseEntity<List<TipoSolicitudPetroleraDTO>> listarActivasPorPetroleraYTipo(
            @PathVariable Long petroleraId,
            @PathVariable Long tipoContratoId) {
        log.info("GET /api/tipos-solicitud-petrolera/petrolera/{}/tipo/{}/activas - Listar activas",
            petroleraId, tipoContratoId);
        return ResponseEntity.ok(tipoSolicitudPetroleraService.listarActivasPorPetroleraYTipo(petroleraId, tipoContratoId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<TipoSolicitudPetroleraDTO> crear(@Valid @RequestBody TipoSolicitudPetroleraDTO dto) {
        log.info("POST /api/tipos-solicitud-petrolera - Crear nuevo tipo");
        TipoSolicitudPetroleraDTO creado = tipoSolicitudPetroleraService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoSolicitudPetroleraDTO> actualizar(@PathVariable Long id,
                                                                @Valid @RequestBody TipoSolicitudPetroleraDTO dto) {
        log.info("PUT /api/tipos-solicitud-petrolera/{} - Actualizar", id);
        return ResponseEntity.ok(tipoSolicitudPetroleraService.actualizar(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/tipos-solicitud-petrolera/{} - Eliminar", id);
        tipoSolicitudPetroleraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        log.info("PATCH /api/tipos-solicitud-petrolera/{}/activar", id);
        tipoSolicitudPetroleraService.activar(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("PATCH /api/tipos-solicitud-petrolera/{}/desactivar", id);
        tipoSolicitudPetroleraService.desactivar(id);
        return ResponseEntity.ok().build();
    }
}
