package com.manuhd.app.dispositivos.controller;

import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.service.SolicitudDispositivoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes-dispositivo")
@Slf4j
public class SolicitudDispositivoController {

    @Autowired
    private SolicitudDispositivoService solicitudService;

    @GetMapping
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarTodas() {
        log.info("GET /api/solicitudes-dispositivo - Listar todas");
        return ResponseEntity.ok(solicitudService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDispositivoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/solicitudes-dispositivo/{}", id);
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/solicitudes-dispositivo/socio/{}", socioId);
        return ResponseEntity.ok(solicitudService.listarPorSocio(socioId));
    }

    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/solicitudes-dispositivo/petrolera/{}", petroleraId);
        return ResponseEntity.ok(solicitudService.listarPorPetrolera(petroleraId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorEstado(@PathVariable EstadoSolicitud estado) {
        log.info("GET /api/solicitudes-dispositivo/estado/{}", estado);
        return ResponseEntity.ok(solicitudService.listarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<SolicitudDispositivoDTO> crear(@Valid @RequestBody CrearSolicitudDTO dto) {
        log.info("POST /api/solicitudes-dispositivo - Crear solicitud tipo {}", dto.getTipoSolicitud());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.crear(dto));
    }

    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<SolicitudDispositivoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/enviar-petrolera", id);
        return ResponseEntity.ok(solicitudService.enviarAPetrolera(id));
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<SolicitudDispositivoDTO> responderPetrolera(
            @PathVariable Long id,
            @RequestBody Map<String, Object> respuesta) {
        log.info("POST /api/solicitudes-dispositivo/{}/responder", id);
        boolean aprobado = (boolean) respuesta.get("aprobado");
        String comentario = (String) respuesta.get("respuesta");
        return ResponseEntity.ok(solicitudService.responderPetrolera(id, aprobado, comentario));
    }

    @PostMapping("/{id}/notificar-socio")
    public ResponseEntity<SolicitudDispositivoDTO> notificarSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/notificar-socio", id);
        return ResponseEntity.ok(solicitudService.notificarSocio(id));
    }
}
