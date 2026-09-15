package com.manuhd.app.dispositivos.controller;

import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.ResponderPetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.service.SolicitudDispositivoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    // ---- Circuito del documento firmado ----

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/pdf/editable")
    public ResponseEntity<SolicitudDispositivoDTO> guardarPdfEditado(@PathVariable Long id,
                                                                     @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes-dispositivo/{}/pdf/editable - Guardar impreso editado", id);
        try {
            return ResponseEntity.ok(solicitudService.guardarPdfEditado(id, file));
        } catch (IOException e) {
            log.error("Error al guardar el impreso editado", e);
            throw new RuntimeException("Error al guardar el impreso: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-socio")
    public ResponseEntity<SolicitudDispositivoDTO> enviarASocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/enviar-socio - Enviar el impreso al socio para su firma", id);
        try {
            return ResponseEntity.ok(solicitudService.enviarASocio(id));
        } catch (IOException e) {
            log.error("Error al enviar el impreso al socio", e);
            throw new RuntimeException("Error al enviar el impreso al socio: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/pdf/firmado")
    public ResponseEntity<SolicitudDispositivoDTO> subirPdfFirmado(@PathVariable Long id,
                                                                   @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes-dispositivo/{}/pdf/firmado - Registrar el impreso firmado por el socio", id);
        try {
            return ResponseEntity.ok(solicitudService.subirPdfFirmado(id, file));
        } catch (IOException e) {
            log.error("Error al registrar el impreso firmado", e);
            throw new RuntimeException("Error al registrar el impreso firmado: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/aceptar-firma")
    public ResponseEntity<SolicitudDispositivoDTO> aceptarFirmaSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/aceptar-firma - Aceptar la firma del socio", id);
        return ResponseEntity.ok(solicitudService.aceptarFirmaSocio(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<SolicitudDispositivoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/enviar-petrolera", id);
        try {
            return ResponseEntity.ok(solicitudService.enviarAPetrolera(id));
        } catch (IOException e) {
            log.error("Error al presentar la solicitud a la petrolera", e);
            throw new RuntimeException("Error al presentar la solicitud a la petrolera: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}/pdf/{tipo}")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id, @PathVariable String tipo) {
        log.info("GET /api/solicitudes-dispositivo/{}/pdf/{} - Descargar el impreso de una etapa", id, tipo);
        SolicitudDispositivoService.TipoPdf tipoPdf;
        try {
            tipoPdf = SolicitudDispositivoService.TipoPdf.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Tipo de PDF invalido: {}", tipo);
            throw new RuntimeException("Tipo de PDF invalido: " + tipo);
        }

        try {
            byte[] pdf = solicitudService.descargarPdf(id, tipoPdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo + ".pdf\"")
                    .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar el impreso", e);
            throw new RuntimeException("Error al descargar el impreso: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/responder")
    public ResponseEntity<SolicitudDispositivoDTO> responderPetrolera(
            @PathVariable Long id,
            @Valid @RequestBody ResponderPetroleraDTO dto) {
        log.info("POST /api/solicitudes-dispositivo/{}/responder", id);
        return ResponseEntity.ok(solicitudService.responderPetrolera(id, dto.getAprobado(), dto.getRespuesta(), dto.getMontoConcedido()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/notificar-socio")
    public ResponseEntity<SolicitudDispositivoDTO> notificarSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-dispositivo/{}/notificar-socio", id);
        return ResponseEntity.ok(solicitudService.notificarSocio(id));
    }
}
