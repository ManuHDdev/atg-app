package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.dto.TipoSolicitudDTO;
import com.manuhd.app.petroleras.service.TipoSolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-solicitud")
@RequiredArgsConstructor
@Slf4j
public class TipoSolicitudController {

    private final TipoSolicitudService tipoSolicitudService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<TipoSolicitudDTO>> listarTodos() {
        log.info("GET /api/tipos-solicitud - Listar todos");
        return ResponseEntity.ok(tipoSolicitudService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<TipoSolicitudDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/tipos-solicitud/petrolera/{} - Listar por petrolera", petroleraId);
        return ResponseEntity.ok(tipoSolicitudService.listarPorPetrolera(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}/activas")
    public ResponseEntity<List<TipoSolicitudDTO>> listarActivasPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/tipos-solicitud/petrolera/{}/activas - Listar activas por petrolera", petroleraId);
        return ResponseEntity.ok(tipoSolicitudService.listarActivasPorPetrolera(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<TipoSolicitudDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/tipos-solicitud/{} - Obtener por ID", id);
        return ResponseEntity.ok(tipoSolicitudService.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<TipoSolicitudDTO> crear(@Valid @RequestBody TipoSolicitudDTO dto) {
        log.info("POST /api/tipos-solicitud - Crear nuevo tipo de solicitud");
        TipoSolicitudDTO creado = tipoSolicitudService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoSolicitudDTO> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody TipoSolicitudDTO dto) {
        log.info("PUT /api/tipos-solicitud/{} - Actualizar", id);
        return ResponseEntity.ok(tipoSolicitudService.actualizar(id, dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/tipos-solicitud/{} - Eliminar", id);
        tipoSolicitudService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/plantilla")
    public ResponseEntity<TipoSolicitudDTO> subirPlantillaPdf(@PathVariable Long id,
                                                               @RequestParam("archivo") MultipartFile archivo) {
        log.info("POST /api/tipos-solicitud/{}/plantilla - Subir plantilla PDF", id);

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!archivo.getContentType().equals("application/pdf")) {
            log.error("Archivo no es PDF: {}", archivo.getContentType());
            return ResponseEntity.badRequest().build();
        }

        try {
            TipoSolicitudDTO actualizado = tipoSolicitudService.subirPlantillaPdf(id, archivo);
            return ResponseEntity.ok(actualizado);
        } catch (IOException e) {
            log.error("Error al subir plantilla PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}/plantilla")
    public ResponseEntity<Void> eliminarPlantillaPdf(@PathVariable Long id) {
        log.info("DELETE /api/tipos-solicitud/{}/plantilla - Eliminar plantilla PDF", id);
        tipoSolicitudService.eliminarPlantillaPdf(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaPdf(@PathVariable Long id) {
        log.info("GET /api/tipos-solicitud/{}/plantilla - Descargar plantilla PDF", id);
        try {
            byte[] pdf = tipoSolicitudService.descargarPlantillaPdf(id);
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"plantilla.pdf\"")
                .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar plantilla PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
