package com.manuhd.app.dispositivos.controller;

import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.ResponderPetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.service.SolicitudDispositivoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-dispositivo")
@Slf4j
public class SolicitudDispositivoController {

    @Autowired
    private SolicitudDispositivoService solicitudService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarTodas() {
        log.info("GET /api/solicitudes-dispositivo - Listar todas");
        return ResponseEntity.ok(solicitudService.listarTodas());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDispositivoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/solicitudes-dispositivo/{}", id);
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/solicitudes-dispositivo/socio/{}", socioId);
        return ResponseEntity.ok(solicitudService.listarPorSocio(socioId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/solicitudes-dispositivo/petrolera/{}", petroleraId);
        return ResponseEntity.ok(solicitudService.listarPorPetrolera(petroleraId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitudDispositivoDTO>> listarPorEstado(@PathVariable EstadoSolicitud estado) {
        log.info("GET /api/solicitudes-dispositivo/estado/{}", estado);
        return ResponseEntity.ok(solicitudService.listarPorEstado(estado));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<SolicitudDispositivoDTO> crear(@Valid @RequestBody CrearSolicitudDTO dto) {
        log.info("POST /api/solicitudes-dispositivo - Crear solicitud tipo {}", dto.getTipoSolicitud());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.crear(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<SolicitudDispositivoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/enviar-petrolera", id);
        return ResponseEntity.ok(solicitudService.enviarAPetrolera(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/responder")
    public ResponseEntity<SolicitudDispositivoDTO> responderPetrolera(
            @PathVariable Long id,
            @Valid @RequestBody ResponderPetroleraDTO dto) {
        log.info("POST /api/solicitudes-dispositivo/{}/responder", id);
        return ResponseEntity.ok(solicitudService.responderPetrolera(id, dto.getAprobado(), dto.getRespuesta()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/notificar-socio")
    public ResponseEntity<SolicitudDispositivoDTO> notificarSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/notificar-socio", id);
        return ResponseEntity.ok(solicitudService.notificarSocio(id));
    }
}
