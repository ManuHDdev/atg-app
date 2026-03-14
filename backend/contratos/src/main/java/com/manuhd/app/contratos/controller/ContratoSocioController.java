package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.model.ContratoSocio;
import com.manuhd.app.contratos.model.EstadoContrato;
import com.manuhd.app.contratos.service.ContratoSocioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoSocioController {

    private final ContratoSocioService contratoService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<ContratoSocio>> getAllContratos() {
        return ResponseEntity.ok(contratoService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<ContratoSocio> getContratoById(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<ContratoSocio>> getContratosBySocioId(@PathVariable Long socioId) {
        return ResponseEntity.ok(contratoService.findBySocioId(socioId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<ContratoSocio>> getContratosByPetroleraId(@PathVariable Long petroleraId) {
        return ResponseEntity.ok(contratoService.findByPetroleraId(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ContratoSocio>> getContratosByEstado(@PathVariable EstadoContrato estado) {
        return ResponseEntity.ok(contratoService.findByEstado(estado));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}/petrolera/{petroleraId}/activos")
    public ResponseEntity<List<ContratoSocio>> getContratosActivosBySocioAndPetrolera(
            @PathVariable Long socioId,
            @PathVariable Long petroleraId) {
        return ResponseEntity.ok(contratoService.findActivosBySocioAndPetrolera(socioId, petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}/petroleras-activas")
    public ResponseEntity<List<Long>> getPetrolerasActivasBySocio(@PathVariable Long socioId) {
        return ResponseEntity.ok(contratoService.findPetrolerasConContratosActivosBySocio(socioId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<ContratoSocio> createContrato(@Valid @RequestBody ContratoSocio contrato) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.create(contrato));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/generar-borrador")
    public ResponseEntity<ContratoSocio> generarBorrador(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> datosAdicionales) {
        try {
            ContratoSocio contrato = contratoService.generarBorrador(id, datosAdicionales);
            return ResponseEntity.ok(contrato);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-socio")
    public ResponseEntity<ContratoSocio> enviarASocio(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.enviarASocio(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/recibir-firmado")
    public ResponseEntity<ContratoSocio> recibirFirmado(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String rutaFirmado = request.get("rutaFirmado");
        return ResponseEntity.ok(contratoService.recibirFirmado(id, rutaFirmado));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<ContratoSocio> enviarAPetrolera(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.enviarAPetrolera(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/completar")
    public ResponseEntity<ContratoSocio> completar(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String rutaFinal = request.get("rutaFinal");
        return ResponseEntity.ok(contratoService.completar(id, rutaFinal));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ContratoSocio> cancelar(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String observaciones = request.get("observaciones");
        return ResponseEntity.ok(contratoService.cancelar(id, observaciones));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ContratoSocio> updateContrato(
            @PathVariable Long id,
            @Valid @RequestBody ContratoSocio contrato) {
        return ResponseEntity.ok(contratoService.update(id, contrato));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContrato(@PathVariable Long id) {
        contratoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
