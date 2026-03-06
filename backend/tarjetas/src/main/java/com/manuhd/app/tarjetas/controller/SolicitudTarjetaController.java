package com.manuhd.app.tarjetas.controller;

import com.manuhd.app.tarjetas.dto.AprobarBajaDTO;
import com.manuhd.app.tarjetas.dto.AprobarDuplicadoDTO;
import com.manuhd.app.tarjetas.dto.CrearSolicitudDTO;
import com.manuhd.app.tarjetas.dto.MarcarEntregadaDTO;
import com.manuhd.app.tarjetas.dto.RegistrarLlegadaDTO;
import com.manuhd.app.tarjetas.dto.SolicitudTarjetaDTO;
import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.service.SolicitudTarjetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes-tarjetas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class SolicitudTarjetaController {

    private final SolicitudTarjetaService service;

    @GetMapping
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarTodas() {
        log.info("GET /api/solicitudes-tarjetas - Listar todas las solicitudes");
        List<SolicitudTarjetaDTO> solicitudes = service.findAll();
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudTarjetaDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/solicitudes-tarjetas/{} - Obtener solicitud por ID", id);
        SolicitudTarjetaDTO solicitud = service.findById(id);
        return ResponseEntity.ok(solicitud);
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/solicitudes-tarjetas/socio/{} - Listar por socio", socioId);
        List<SolicitudTarjetaDTO> solicitudes = service.findBySocioId(socioId);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarPorEstado(@PathVariable EstadoSolicitud estado) {
        log.info("GET /api/solicitudes-tarjetas/estado/{} - Listar por estado", estado);
        List<SolicitudTarjetaDTO> solicitudes = service.findByEstado(estado);
        return ResponseEntity.ok(solicitudes);
    }

    @PostMapping
    public ResponseEntity<SolicitudTarjetaDTO> crear(@Valid @RequestBody CrearSolicitudDTO dto) {
        log.info("POST /api/solicitudes-tarjetas - Crear nueva solicitud de tipo: {}", dto.getTipo());
        SolicitudTarjetaDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/completar")
    public ResponseEntity<SolicitudTarjetaDTO> completar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("PUT /api/solicitudes-tarjetas/{}/completar - Completar solicitud", id);
        String procesadoPor = body != null ? body.get("procesadoPor") : "Sistema";
        SolicitudTarjetaDTO completed = service.completar(id, procesadoPor);
        return ResponseEntity.ok(completed);
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudTarjetaDTO> rechazar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("PUT /api/solicitudes-tarjetas/{}/rechazar - Rechazar solicitud", id);
        String motivo = body.getOrDefault("motivo", "Sin especificar");
        String procesadoPor = body.getOrDefault("procesadoPor", "Sistema");
        SolicitudTarjetaDTO rejected = service.rechazar(id, motivo, procesadoPor);
        return ResponseEntity.ok(rejected);
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudTarjetaDTO> aprobar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar - Aprobar solicitud", id);
        String procesadoPor = body != null ? body.getOrDefault("procesadoPor", "Sistema") : "Sistema";
        SolicitudTarjetaDTO approved = service.aprobar(id, procesadoPor);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/aprobar-baja")
    public ResponseEntity<SolicitudTarjetaDTO> aprobarBaja(
            @PathVariable Long id,
            @Valid @RequestBody AprobarBajaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar-baja - Aprobar solicitud de BAJA con fecha: {}", id, dto.getFechaBaja());
        SolicitudTarjetaDTO approved = service.aprobarBaja(id, dto);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/aprobar-duplicado")
    public ResponseEntity<SolicitudTarjetaDTO> aprobarDuplicado(
            @PathVariable Long id,
            @Valid @RequestBody AprobarDuplicadoDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar-duplicado - Aprobar solicitud de DUPLICADO con fecha: {}", id, dto.getFechaRespuesta());
        SolicitudTarjetaDTO approved = service.aprobarDuplicado(id, dto);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/registrar-llegada")
    public ResponseEntity<SolicitudTarjetaDTO> registrarLlegada(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarLlegadaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/registrar-llegada - Registrar llegada", id);
        SolicitudTarjetaDTO updated = service.registrarLlegada(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/marcar-entregada")
    public ResponseEntity<SolicitudTarjetaDTO> marcarEntregada(
            @PathVariable Long id,
            @Valid @RequestBody MarcarEntregadaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/marcar-entregada - Marcar como entregada", id);
        SolicitudTarjetaDTO updated = service.marcarEntregada(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<SolicitudTarjetaDTO> finalizar(@PathVariable Long id) {
        log.info("PUT /api/solicitudes-tarjetas/{}/finalizar - Finalizar solicitud", id);
        SolicitudTarjetaDTO finalized = service.finalizar(id);
        return ResponseEntity.ok(finalized);
    }
}
