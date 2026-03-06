package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.dto.CreditoDTO;
import com.manuhd.app.contratos.dto.CrearCreditoDTO;
import com.manuhd.app.contratos.model.EstadoCredito;
import com.manuhd.app.contratos.service.CreditoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creditos")
@Slf4j
public class CreditoController {

    @Autowired
    private CreditoService creditoService;

    @GetMapping
    public ResponseEntity<List<CreditoDTO>> listarTodos() {
        log.info("GET /api/creditos - Listar todos los créditos");
        return ResponseEntity.ok(creditoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/creditos/{}", id);
        return ResponseEntity.ok(creditoService.obtenerPorId(id));
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<CreditoDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/creditos/socio/{}", socioId);
        return ResponseEntity.ok(creditoService.listarPorSocio(socioId));
    }

    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<CreditoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/creditos/petrolera/{}", petroleraId);
        return ResponseEntity.ok(creditoService.listarPorPetrolera(petroleraId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<CreditoDTO>> listarPorEstado(@PathVariable EstadoCredito estado) {
        log.info("GET /api/creditos/estado/{}", estado);
        return ResponseEntity.ok(creditoService.listarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<CreditoDTO> crear(@Valid @RequestBody CrearCreditoDTO dto) {
        log.info("POST /api/creditos - Crear crédito");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditoService.crear(dto));
    }

    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<CreditoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/creditos/{}/enviar-petrolera", id);
        return ResponseEntity.ok(creditoService.enviarAPetrolera(id));
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<CreditoDTO> responderPetrolera(
            @PathVariable Long id,
            @RequestBody Map<String, Object> respuesta) {
        log.info("POST /api/creditos/{}/responder", id);
        boolean aprobado = (boolean) respuesta.get("aprobado");
        String comentario = (String) respuesta.get("respuesta");
        return ResponseEntity.ok(creditoService.responderPetrolera(id, aprobado, comentario));
    }

    @PostMapping("/{id}/notificar-socio")
    public ResponseEntity<CreditoDTO> notificarSocio(@PathVariable Long id) {
        log.info("POST /api/creditos/{}/notificar-socio", id);
        return ResponseEntity.ok(creditoService.notificarSocio(id));
    }
}
