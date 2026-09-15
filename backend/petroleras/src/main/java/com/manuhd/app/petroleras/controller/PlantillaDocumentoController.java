package com.manuhd.app.petroleras.controller;

import com.manuhd.app.petroleras.dto.PlantillaDocumentoDTO;
import com.manuhd.app.petroleras.enums.ModuloDocumento;
import com.manuhd.app.petroleras.service.PlantillaDocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/**
 * Plantillas PDF de documentos por petrolera y módulo (tarjetas, dispositivos).
 *
 * <p>Almacén independiente del de contratos: no cuelga de TipoSolicitud ni de la
 * jerarquía TipoContrato → TipoSolicitudPetrolera.</p>
 */
@RestController
@RequestMapping("/api/plantillas-documento")
@RequiredArgsConstructor
@Slf4j
public class PlantillaDocumentoController {

    private final PlantillaDocumentoService plantillaDocumentoService;

    @Operation(summary = "Lista todas las plantillas de documento configuradas")
    @ApiResponse(responseCode = "200", description = "Listado de plantillas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping
    public ResponseEntity<List<PlantillaDocumentoDTO>> listarTodas() {
        log.info("GET /api/plantillas-documento - Listar todas");
        return ResponseEntity.ok(plantillaDocumentoService.listarTodas());
    }

    @Operation(summary = "Lista las plantillas de documento de una petrolera")
    @ApiResponse(responseCode = "200", description = "Listado de plantillas de la petrolera")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/petrolera/{petroleraId}")
    public ResponseEntity<List<PlantillaDocumentoDTO>> listarPorPetrolera(@PathVariable Long petroleraId) {
        log.info("GET /api/plantillas-documento/petrolera/{} - Listar por petrolera", petroleraId);
        return ResponseEntity.ok(plantillaDocumentoService.listarPorPetrolera(petroleraId));
    }

    @Operation(summary = "Obtiene una plantilla de documento por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla encontrada"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}")
    public ResponseEntity<PlantillaDocumentoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/plantillas-documento/{}", id);
        return ResponseEntity.ok(plantillaDocumentoService.obtenerPorId(id));
    }

    @Operation(summary = "Busca la plantilla activa de una petrolera, módulo y tipo de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla activa encontrada"),
            @ApiResponse(responseCode = "404", description = "No hay plantilla configurada para esa combinación")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<PlantillaDocumentoDTO> buscar(@RequestParam Long petroleraId,
                                                        @RequestParam ModuloDocumento modulo,
                                                        @RequestParam String tipoSolicitud) {
        log.info("GET /api/plantillas-documento/buscar - petrolera={} modulo={} tipoSolicitud={}",
                petroleraId, modulo, tipoSolicitud);
        return ResponseEntity.ok(plantillaDocumentoService.buscar(petroleraId, modulo, tipoSolicitud));
    }

    @Operation(summary = "Descarga el PDF de una plantilla concreta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF de la plantilla"),
            @ApiResponse(responseCode = "404", description = "Plantilla o archivo no disponible")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/{id}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id) {
        log.info("GET /api/plantillas-documento/{}/archivo", id);
        return respuestaPdf(plantillaDocumentoService.descargarArchivo(id));
    }

    /**
     * Endpoint que consumen los microservicios de tarjetas y dispositivos.
     * Devuelve 404 (no 500) cuando no hay plantilla configurada, para que el consumidor
     * lo traduzca a su error de negocio PLANTILLA_NO_CONFIGURADA.
     */
    @Operation(summary = "Descarga el PDF de la plantilla activa de una petrolera, módulo y tipo de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF de la plantilla activa"),
            @ApiResponse(responseCode = "404", description = "No hay plantilla configurada o el archivo no está disponible")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'USUARIO')")
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarPorClave(@RequestParam Long petroleraId,
                                                    @RequestParam ModuloDocumento modulo,
                                                    @RequestParam String tipoSolicitud) {
        log.info("GET /api/plantillas-documento/descargar - petrolera={} modulo={} tipoSolicitud={}",
                petroleraId, modulo, tipoSolicitud);
        return respuestaPdf(plantillaDocumentoService.descargarArchivo(petroleraId, modulo, tipoSolicitud));
    }

    @Operation(summary = "Sube una nueva plantilla PDF para una petrolera, módulo y tipo de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plantilla creada"),
            @ApiResponse(responseCode = "400", description = "Archivo no válido o plantilla ya existente"),
            @ApiResponse(responseCode = "404", description = "Petrolera no encontrada")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlantillaDocumentoDTO> crear(@RequestParam("file") MultipartFile file,
                                                       @RequestParam Long petroleraId,
                                                       @RequestParam ModuloDocumento modulo,
                                                       @RequestParam String tipoSolicitud) throws IOException {
        log.info("POST /api/plantillas-documento - petrolera={} modulo={} tipoSolicitud={}",
                petroleraId, modulo, tipoSolicitud);
        PlantillaDocumentoDTO creada = plantillaDocumentoService.crear(petroleraId, modulo, tipoSolicitud, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @Operation(summary = "Reemplaza el PDF de una plantilla existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "400", description = "Archivo no válido"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlantillaDocumentoDTO> reemplazarArchivo(@PathVariable Long id,
                                                                   @RequestParam("file") MultipartFile file)
            throws IOException {
        log.info("PUT /api/plantillas-documento/{} - Reemplazar archivo", id);
        return ResponseEntity.ok(plantillaDocumentoService.reemplazarArchivo(id, file));
    }

    @Operation(summary = "Activa o desactiva una plantilla de documento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado no indicado"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PlantillaDocumentoDTO> cambiarEstado(@PathVariable Long id,
                                                               @RequestBody Map<String, Boolean> body) {
        log.info("PATCH /api/plantillas-documento/{}/estado", id);
        return ResponseEntity.ok(plantillaDocumentoService.cambiarEstado(id, body.get("activa")));
    }

    @Operation(summary = "Elimina lógicamente una plantilla de documento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plantilla eliminada"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/plantillas-documento/{}", id);
        plantillaDocumentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"plantilla.pdf\"")
                .body(pdf);
    }
}
