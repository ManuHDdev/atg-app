package com.manuhd.app.creditos.controller;

import com.manuhd.app.creditos.dto.CreditoDTO;
import com.manuhd.app.creditos.dto.CrearCreditoDTO;
import com.manuhd.app.creditos.dto.ResponderPetroleraDTO;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.service.CreditoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creditos")
@Slf4j
public class CreditoController {

    @Autowired
    private CreditoService creditoService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<CreditoDTO>> listarTodos() {
        log.info("GET /api/creditos - Listar todos los créditos");
        return ResponseEntity.ok(creditoService.listarTodos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<CreditoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/creditos/{}", id);
        return ResponseEntity.ok(creditoService.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<CreditoDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/creditos/socio/{}", socioId);
        return ResponseEntity.ok(creditoService.listarPorSocio(socioId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<CreditoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/creditos/petrolera/{}", petroleraId);
        return ResponseEntity.ok(creditoService.listarPorPetrolera(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<CreditoDTO>> listarPorEstado(@PathVariable EstadoCredito estado) {
        log.info("GET /api/creditos/estado/{}", estado);
        return ResponseEntity.ok(creditoService.listarPorEstado(estado));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<CreditoDTO> crear(@Valid @RequestBody CrearCreditoDTO dto) {
        log.info("POST /api/creditos - Crear crédito");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditoService.crear(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<CreditoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/creditos/{}/enviar-petrolera", id);
        return ResponseEntity.ok(creditoService.enviarAPetrolera(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/responder")
    public ResponseEntity<CreditoDTO> responderPetrolera(
            @PathVariable Long id,
            @Valid @RequestBody ResponderPetroleraDTO dto) {
        log.info("POST /api/creditos/{}/responder", id);
        return ResponseEntity.ok(creditoService.responderPetrolera(id, dto.getAprobado(), dto.getRespuesta()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/notificar-socio")
    public ResponseEntity<CreditoDTO> notificarSocio(@PathVariable Long id) {
        log.info("POST /api/creditos/{}/notificar-socio", id);
        return ResponseEntity.ok(creditoService.notificarSocio(id));
    }
}
