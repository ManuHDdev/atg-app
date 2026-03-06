package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.dto.CrearSolicitudDTO;
import com.manuhd.app.contratos.dto.FiltroSolicitudesDTO;
import com.manuhd.app.contratos.dto.SolicitudContratoDTO;
import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.service.SolicitudContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class SolicitudContratoController {

    private final SolicitudContratoService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudContratoDTO> crearSolicitud(@Valid @RequestBody CrearSolicitudDTO dto) {
        log.info("POST /api/solicitudes - Crear nueva solicitud");
        try {
            SolicitudContratoDTO creada = solicitudService.crearSolicitud(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IOException e) {
            log.error("Error al crear solicitud", e);
            throw new RuntimeException("Error al crear solicitud: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<SolicitudContratoDTO>> listarSolicitudes(
            @RequestParam(required = false) Long socioId,
            @RequestParam(required = false) Long petroleraId,
            @RequestParam(required = false) Long tipoContratoId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("GET /api/solicitudes - Listar con filtros");

        FiltroSolicitudesDTO filtros = new FiltroSolicitudesDTO();
        filtros.setSocioId(socioId);
        filtros.setPetroleraId(petroleraId);
        filtros.setTipoContratoId(tipoContratoId);
        filtros.setEstado(estado);
        filtros.setPage(page);
        filtros.setSize(size);
        filtros.setSortBy(sortBy);
        filtros.setSortDirection(sortDirection);

        return ResponseEntity.ok(solicitudService.listarSolicitudes(filtros));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudContratoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/solicitudes/{} - Obtener por ID", id);
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    @GetMapping("/numero/{numeroSolicitud}")
    public ResponseEntity<SolicitudContratoDTO> obtenerPorNumero(@PathVariable String numeroSolicitud) {
        log.info("GET /api/solicitudes/numero/{} - Obtener por número", numeroSolicitud);
        return ResponseEntity.ok(solicitudService.obtenerPorNumeroSolicitud(numeroSolicitud));
    }

    @GetMapping("/{id}/pdf/editable")
    public ResponseEntity<byte[]> descargarPdfEditable(@PathVariable Long id) {
        log.info("GET /api/solicitudes/{}/pdf/editable - Descargar PDF editable", id);
        try {
            byte[] pdf = solicitudService.abrirPdfEditable(id);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"editable.pdf\"")
                .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar PDF editable", e);
            throw new RuntimeException("Error al descargar PDF: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf/plantilla-original")
    public ResponseEntity<byte[]> descargarPlantillaOriginal(@PathVariable Long id) {
        log.info("GET /api/solicitudes/{}/pdf/plantilla-original - Descargar plantilla original", id);
        try {
            byte[] pdf = solicitudService.abrirPlantillaOriginal(id);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_original.pdf\"")
                .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar plantilla original", e);
            throw new RuntimeException("Error al descargar plantilla original: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pdf/editable")
    public ResponseEntity<Void> guardarPdfEditado(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes/{}/pdf/editable - Guardar PDF editado", id);
        try {
            solicitudService.guardarPdfEditado(id, file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Error al guardar PDF editado", e);
            throw new RuntimeException("Error al guardar PDF: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/enviar-socio")
    public ResponseEntity<SolicitudContratoDTO> enviarASocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes/{}/enviar-socio - Enviar a socio", id);
        try {
            SolicitudContratoDTO actualizada = solicitudService.enviarASocio(id);
            return ResponseEntity.ok(actualizada);
        } catch (IOException e) {
            log.error("Error al enviar a socio", e);
            throw new RuntimeException("Error al enviar a socio: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pdf/firmado")
    public ResponseEntity<SolicitudContratoDTO> subirPdfFirmado(@PathVariable Long id,
                                                                @RequestParam("file") MultipartFile file) {
        log.info("POST /api/solicitudes/{}/pdf/firmado - Subir PDF firmado", id);
        try {
            SolicitudContratoDTO actualizada = solicitudService.subirPdfFirmado(id, file);
            return ResponseEntity.ok(actualizada);
        } catch (IOException e) {
            log.error("Error al subir PDF firmado", e);
            throw new RuntimeException("Error al subir PDF firmado: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/enviar-petrolera")
    public ResponseEntity<SolicitudContratoDTO> enviarAPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes/{}/enviar-petrolera - Enviar a petrolera", id);
        try {
            SolicitudContratoDTO actualizada = solicitudService.enviarAPetrolera(id);
            return ResponseEntity.ok(actualizada);
        } catch (IOException e) {
            log.error("Error al enviar a petrolera", e);
            throw new RuntimeException("Error al enviar a petrolera: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/aceptar-firma")
    public ResponseEntity<SolicitudContratoDTO> aceptarFirmaSocio(@PathVariable Long id) {
        log.info("POST /api/solicitudes/{}/aceptar-firma - Aceptar firma del socio", id);
        SolicitudContratoDTO actualizada = solicitudService.aceptarFirmaSocio(id);
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/aceptar-petrolera")
    public ResponseEntity<SolicitudContratoDTO> aceptarPorPetrolera(@PathVariable Long id) {
        log.info("POST /api/solicitudes/{}/aceptar-petrolera - Aceptar por petrolera", id);
        SolicitudContratoDTO actualizada = solicitudService.aceptarPorPetrolera(id);
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/rechazar-petrolera")
    public ResponseEntity<SolicitudContratoDTO> rechazarPorPetrolera(@PathVariable Long id,
                                                                      @RequestBody java.util.Map<String, String> body) {
        log.info("POST /api/solicitudes/{}/rechazar-petrolera - Rechazar por petrolera", id);
        String motivoRechazo = body.getOrDefault("motivoRechazo", "Sin motivo especificado");
        SolicitudContratoDTO actualizada = solicitudService.rechazarPorPetrolera(id, motivoRechazo);
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/procesar-baja")
    public ResponseEntity<SolicitudContratoDTO> procesarBaja(@PathVariable Long id) {
        log.info("POST /api/solicitudes/{}/procesar-baja - Procesar solicitud de BAJA", id);
        SolicitudContratoDTO actualizada = solicitudService.procesarBaja(id);
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/{id}/pdf/{tipo}")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id,
                                               @PathVariable String tipo) {
        log.info("GET /api/solicitudes/{}/pdf/{} - Descargar PDF", id, tipo);
        try {
            SolicitudContratoService.TipoPdf tipoPdf = SolicitudContratoService.TipoPdf.valueOf(tipo.toUpperCase());
            byte[] pdf = solicitudService.descargarPdf(id, tipoPdf);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo + ".pdf\"")
                .body(pdf);
        } catch (IOException e) {
            log.error("Error al descargar PDF", e);
            throw new RuntimeException("Error al descargar PDF: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Tipo de PDF inválido: {}", tipo);
            throw new RuntimeException("Tipo de PDF inválido: " + tipo);
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<SolicitudContratoDTO> cambiarEstado(@PathVariable Long id,
                                                              @RequestParam EstadoSolicitud estado) {
        log.info("PUT /api/solicitudes/{}/estado - Cambiar estado a {}", id, estado);
        SolicitudContratoDTO actualizada = solicitudService.cambiarEstado(id, estado);
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/{id}/pdf/campos")
    public ResponseEntity<?> obtenerCamposPdf(@PathVariable Long id) {
        log.info("GET /api/solicitudes/{}/pdf/campos - Obtener campos del PDF", id);
        try {
            return ResponseEntity.ok(solicitudService.extraerCamposPdf(id));
        } catch (IOException e) {
            log.error("Error al extraer campos del PDF", e);
            throw new RuntimeException("Error al extraer campos del PDF: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pdf/rellenar")
    public ResponseEntity<byte[]> rellenarPdf(@PathVariable Long id,
                                              @RequestBody java.util.Map<String, String> campos) {
        log.info("POST /api/solicitudes/{}/pdf/rellenar - Rellenar PDF con campos", id);
        try {
            byte[] pdf = solicitudService.rellenarCamposPdf(id, campos);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (IOException e) {
            log.error("Error al rellenar PDF", e);
            throw new RuntimeException("Error al rellenar PDF: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf/preview")
    public ResponseEntity<?> obtenerPdfPreview(@PathVariable Long id) {
        log.info("GET /api/solicitudes/{}/pdf/preview - Obtener PDF en base64 para preview", id);
        try {
            return ResponseEntity.ok(solicitudService.obtenerPdfBase64(id));
        } catch (IOException e) {
            log.error("Error al obtener PDF preview", e);
            throw new RuntimeException("Error al obtener PDF preview: " + e.getMessage());
        }
    }

    @GetMapping("/plantilla/{tipoSolicitudId}/campos")
    public ResponseEntity<?> obtenerCamposPlantilla(@PathVariable Long tipoSolicitudId) {
        log.info("GET /api/solicitudes/plantilla/{}/campos - Obtener campos de la plantilla PDF", tipoSolicitudId);
        try {
            return ResponseEntity.ok(solicitudService.extraerCamposPlantilla(tipoSolicitudId));
        } catch (IOException e) {
            log.error("Error al extraer campos de la plantilla", e);
            throw new RuntimeException("Error al extraer campos de la plantilla: " + e.getMessage());
        }
    }

    @GetMapping("/plantilla/{tipoSolicitudId}/preview")
    public ResponseEntity<?> obtenerPlantillaPreview(@PathVariable Long tipoSolicitudId) {
        log.info("GET /api/solicitudes/plantilla/{}/preview - Obtener plantilla PDF en base64", tipoSolicitudId);
        try {
            return ResponseEntity.ok(solicitudService.obtenerPlantillaBase64(tipoSolicitudId));
        } catch (IOException e) {
            log.error("Error al obtener plantilla preview", e);
            throw new RuntimeException("Error al obtener plantilla preview: " + e.getMessage());
        }
    }
}
