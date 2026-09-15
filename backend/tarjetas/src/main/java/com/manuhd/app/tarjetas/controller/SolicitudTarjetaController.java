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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes-tarjetas")
@RequiredArgsConstructor
@Slf4j
public class SolicitudTarjetaController {

    private final SolicitudTarjetaService service;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarTodas() {
        log.info("GET /api/solicitudes-tarjetas - Listar todas las solicitudes");
        List<SolicitudTarjetaDTO> solicitudes = service.findAll();
        return ResponseEntity.ok(solicitudes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudTarjetaDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/solicitudes-tarjetas/{} - Obtener solicitud por ID", id);
        SolicitudTarjetaDTO solicitud = service.findById(id);
        return ResponseEntity.ok(solicitud);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarPorSocio(@PathVariable Long socioId) {
        log.info("GET /api/solicitudes-tarjetas/socio/{} - Listar por socio", socioId);
        List<SolicitudTarjetaDTO> solicitudes = service.findBySocioId(socioId);
        return ResponseEntity.ok(solicitudes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitudTarjetaDTO>> listarPorEstado(@PathVariable EstadoSolicitud estado) {
        log.info("GET /api/solicitudes-tarjetas/estado/{} - Listar por estado", estado);
        List<SolicitudTarjetaDTO> solicitudes = service.findByEstado(estado);
        return ResponseEntity.ok(solicitudes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping
    public ResponseEntity<SolicitudTarjetaDTO> crear(@Valid @RequestBody CrearSolicitudDTO dto) {
        log.info("POST /api/solicitudes-tarjetas - Crear nueva solicitud de tipo: {}", dto.getTipo());
        SolicitudTarjetaDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/denegar-petrolera")
    public ResponseEntity<SolicitudTarjetaDTO> denegarPorPetrolera(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("PUT /api/solicitudes-tarjetas/{}/denegar-petrolera - Registrar denegación de la petrolera", id);
        String motivo = body.getOrDefault("motivo", "Sin especificar");
        SolicitudTarjetaDTO denegada = service.denegarPorPetrolera(id, motivo);
        return ResponseEntity.ok(denegada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/aprobar-petrolera")
    public ResponseEntity<SolicitudTarjetaDTO> aprobarPorPetrolera(@PathVariable Long id) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar-petrolera - Registrar aprobación de la petrolera", id);
        SolicitudTarjetaDTO aprobada = service.aprobarPorPetrolera(id);
        return ResponseEntity.ok(aprobada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/aprobar-baja-petrolera")
    public ResponseEntity<SolicitudTarjetaDTO> aprobarBajaPorPetrolera(
            @PathVariable Long id,
            @Valid @RequestBody AprobarBajaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar-baja-petrolera - Registrar aprobación de BAJA por la petrolera con fecha: {}", id, dto.getFechaBaja());
        SolicitudTarjetaDTO aprobada = service.aprobarBajaPorPetrolera(id, dto);
        return ResponseEntity.ok(aprobada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/aprobar-duplicado-petrolera")
    public ResponseEntity<SolicitudTarjetaDTO> aprobarDuplicadoPorPetrolera(
            @PathVariable Long id,
            @Valid @RequestBody AprobarDuplicadoDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/aprobar-duplicado-petrolera - Registrar aprobación de DUPLICADO por la petrolera con fecha: {}", id, dto.getFechaRespuesta());
        SolicitudTarjetaDTO aprobada = service.aprobarDuplicadoPorPetrolera(id, dto);
        return ResponseEntity.ok(aprobada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/registrar-llegada")
    public ResponseEntity<SolicitudTarjetaDTO> registrarLlegada(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarLlegadaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/registrar-llegada - Registrar llegada", id);
        SolicitudTarjetaDTO updated = service.registrarLlegada(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping("/{id}/marcar-entregada")
    public ResponseEntity<SolicitudTarjetaDTO> marcarEntregada(
            @PathVariable Long id,
            @Valid @RequestBody MarcarEntregadaDTO dto) {
        log.info("PUT /api/solicitudes-tarjetas/{}/marcar-entregada - Marcar como entregada y completar", id);
        SolicitudTarjetaDTO updated = service.marcarEntregada(id, dto);
        return ResponseEntity.ok(updated);
    }

    // ---------- circuito del documento firmado ----------

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/pdf/editable")
    public ResponseEntity<SolicitudTarjetaDTO> guardarPdfEditado(@PathVariable Long id,
                                                                 @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes-tarjetas/{}/pdf/editable - Guardar impreso editado", id);
        try {
            return ResponseEntity.ok(service.guardarPdfEditado(id, file));
        } catch (IOException e) {
            log.error("Error al guardar el impreso editado", e);
            throw new RuntimeException("Error al guardar el impreso: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-socio")
    public ResponseEntity<SolicitudTarjetaDTO> enviarASocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-tarjetas/{}/enviar-socio - Enviar el impreso al socio para su firma", id);
        try {
            return ResponseEntity.ok(service.enviarASocio(id));
        } catch (IOException e) {
            log.error("Error al enviar el impreso al socio", e);
            throw new RuntimeException("Error al enviar el impreso al socio: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/pdf/firmado")
    public ResponseEntity<SolicitudTarjetaDTO> subirPdfFirmado(@PathVariable Long id,
                                                               @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes-tarjetas/{}/pdf/firmado - Registrar el impreso firmado por el socio", id);
        try {
            return ResponseEntity.ok(service.subirPdfFirmado(id, file));
        } catch (IOException e) {
            log.error("Error al registrar el impreso firmado", e);
            throw new RuntimeException("Error al registrar el impreso firmado: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/aceptar-firma")
    public ResponseEntity<SolicitudTarjetaDTO> aceptarFirmaSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes-tarjetas/{}/aceptar-firma - Aceptar la firma del socio", id);
        return ResponseEntity.ok(service.aceptarFirmaSocio(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<SolicitudTarjetaDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes-tarjetas/{}/enviar-petrolera - Presentar la solicitud a la petrolera", id);
        try {
            return ResponseEntity.ok(service.enviarAPetrolera(id));
        } catch (IOException e) {
            log.error("Error al presentar la solicitud a la petrolera", e);
            throw new RuntimeException("Error al presentar la solicitud a la petrolera: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}/pdf/{tipo}")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id, @PathVariable String tipo) {
        log.info("GET /api/solicitudes-tarjetas/{}/pdf/{} - Descargar el impreso de una etapa", id, tipo);
        SolicitudTarjetaService.TipoPdf tipoPdf;
        try {
            tipoPdf = SolicitudTarjetaService.TipoPdf.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Tipo de PDF inválido: {}", tipo);
            throw new RuntimeException("Tipo de PDF inválido: " + tipo);
        }

        try {
            byte[] pdf = service.descargarPdf(id, tipoPdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo + ".pdf\"")
                    .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar el impreso", e);
            throw new RuntimeException("Error al descargar el impreso: " + e.getMessage());
        }
    }
}
