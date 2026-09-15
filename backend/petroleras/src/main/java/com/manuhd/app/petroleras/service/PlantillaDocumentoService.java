package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.dto.PlantillaDocumentoDTO;
import com.manuhd.app.petroleras.enums.ModuloDocumento;
import com.manuhd.app.petroleras.exception.BusinessValidationException;
import com.manuhd.app.petroleras.exception.ResourceNotFoundException;
import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.model.PlantillaDocumento;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import com.manuhd.app.petroleras.repository.PlantillaDocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestiona las plantillas PDF por petrolera, módulo y tipo de solicitud.
 *
 * <p>Los ficheros se guardan bajo {@code app.storage.plantillas-documento-path}, directorio
 * hermano del que ya usa TipoSolicitudService dentro del mismo volumen
 * ({@code petroleras_storage:/app/storage}).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantillaDocumentoService {

    /** Firma de un fichero PDF: los cinco primeros bytes son siempre "%PDF-". */
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final PlantillaDocumentoRepository plantillaDocumentoRepository;
    private final PetroleraRepository petroleraRepository;

    @Value("${app.storage.plantillas-documento-path:storage/plantillas-documento}")
    private String plantillasBasePath;

    @Transactional(readOnly = true)
    public List<PlantillaDocumentoDTO> listarTodas() {
        log.info("Listando todas las plantillas de documento");
        return plantillaDocumentoRepository.findByActivoTrueOrderByPetroleraIdAscModuloAscTipoSolicitudAsc()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlantillaDocumentoDTO> listarPorPetrolera(Long petroleraId) {
        log.info("Listando plantillas de documento de la petrolera ID: {}", petroleraId);
        return plantillaDocumentoRepository
                .findByPetroleraIdAndActivoTrueOrderByModuloAscTipoSolicitudAsc(petroleraId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantillaDocumentoDTO obtenerPorId(Long id) {
        return convertirADTO(buscarViva(id));
    }

    /**
     * Devuelve la plantilla activa de una clave. Lanza ResourceNotFoundException (404)
     * si no hay ninguna configurada: es el contrato que esperan tarjetas y dispositivos.
     */
    @Transactional(readOnly = true)
    public PlantillaDocumentoDTO buscar(Long petroleraId, ModuloDocumento modulo, String tipoSolicitud) {
        return convertirADTO(buscarActivaOFallar(petroleraId, modulo, tipoSolicitud));
    }

    @Transactional(readOnly = true)
    public byte[] descargarArchivo(Long id) {
        return leerArchivo(buscarViva(id));
    }

    /**
     * Endpoint de consumo para tarjetas y dispositivos: devuelve el PDF de la plantilla
     * activa de la clave, o 404 si no está configurada o el fichero no está en disco.
     */
    @Transactional(readOnly = true)
    public byte[] descargarArchivo(Long petroleraId, ModuloDocumento modulo, String tipoSolicitud) {
        return leerArchivo(buscarActivaOFallar(petroleraId, modulo, tipoSolicitud));
    }

    @Transactional
    public PlantillaDocumentoDTO crear(Long petroleraId, ModuloDocumento modulo, String tipoSolicitud,
                                       MultipartFile archivo) throws IOException {
        log.info("Creando plantilla de documento petrolera={} modulo={} tipoSolicitud={}",
                petroleraId, modulo, tipoSolicitud);

        validarPetroleraExiste(petroleraId);
        String tipoNormalizado = normalizarTipoSolicitud(tipoSolicitud);
        validarPdf(archivo);

        plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivoTrue(petroleraId, modulo, tipoNormalizado)
                .ifPresent(existente -> {
                    throw new BusinessValidationException(
                            "Ya existe una plantilla para esta petrolera, módulo y tipo de solicitud. "
                                    + "Reemplace el archivo de la plantilla existente o elimínela primero.");
                });

        PlantillaDocumento plantilla = PlantillaDocumento.builder()
                .petroleraId(petroleraId)
                .modulo(modulo)
                .tipoSolicitud(tipoNormalizado)
                .activa(true)
                .activo(true)
                .build();

        guardarArchivo(plantilla, archivo);

        PlantillaDocumento guardada = plantillaDocumentoRepository.save(plantilla);
        log.info("Plantilla de documento creada con ID: {}", guardada.getId());
        return convertirADTO(guardada);
    }

    /** Sustituye el PDF de una plantilla existente y borra el fichero anterior. */
    @Transactional
    public PlantillaDocumentoDTO reemplazarArchivo(Long id, MultipartFile archivo) throws IOException {
        log.info("Reemplazando archivo de la plantilla de documento ID: {}", id);

        PlantillaDocumento plantilla = buscarViva(id);
        validarPdf(archivo);

        String rutaAnterior = plantilla.getRutaArchivo();
        guardarArchivo(plantilla, archivo);
        eliminarArchivoDeDisco(rutaAnterior);

        PlantillaDocumento actualizada = plantillaDocumentoRepository.save(plantilla);
        log.info("Archivo reemplazado en la plantilla de documento ID: {}", id);
        return convertirADTO(actualizada);
    }

    @Transactional
    public PlantillaDocumentoDTO cambiarEstado(Long id, Boolean activa) {
        log.info("Cambiando estado de la plantilla de documento ID: {} a activa={}", id, activa);

        if (activa == null) {
            throw new BusinessValidationException("El estado 'activa' es obligatorio");
        }

        PlantillaDocumento plantilla = buscarViva(id);
        plantilla.setActiva(activa);
        return convertirADTO(plantillaDocumentoRepository.save(plantilla));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando (borrado lógico) la plantilla de documento ID: {}", id);

        PlantillaDocumento plantilla = buscarViva(id);
        plantilla.setActiva(false);
        plantilla.setActivo(false);
        plantilla.setDeletedAt(LocalDateTime.now());
        plantillaDocumentoRepository.save(plantilla);

        log.info("Plantilla de documento eliminada (soft delete) ID: {}", id);
    }

    // ---------- helpers ----------

    private PlantillaDocumento buscarViva(Long id) {
        PlantillaDocumento plantilla = plantillaDocumentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plantilla de documento no encontrada con ID: " + id));

        if (!Boolean.TRUE.equals(plantilla.getActivo())) {
            throw new ResourceNotFoundException("Plantilla de documento no encontrada con ID: " + id);
        }
        return plantilla;
    }

    private PlantillaDocumento buscarActivaOFallar(Long petroleraId, ModuloDocumento modulo, String tipoSolicitud) {
        return plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
                        petroleraId, modulo, normalizarTipoSolicitud(tipoSolicitud))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay plantilla de documento configurada para la petrolera " + petroleraId
                                + ", módulo " + modulo + " y tipo de solicitud " + tipoSolicitud));
    }

    private byte[] leerArchivo(PlantillaDocumento plantilla) {
        if (plantilla.getRutaArchivo() == null || plantilla.getRutaArchivo().isBlank()) {
            throw new ResourceNotFoundException(
                    "La plantilla de documento " + plantilla.getId() + " no tiene archivo PDF asociado");
        }

        Path path = Paths.get(plantilla.getRutaArchivo());
        if (!Files.exists(path)) {
            log.warn("Archivo PDF no encontrado en disco para la plantilla {}: {}",
                    plantilla.getId(), plantilla.getRutaArchivo());
            throw new ResourceNotFoundException(
                    "El archivo PDF de esta plantilla no está disponible. Por favor, vuelva a subirlo.");
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessValidationException(
                    "No se pudo leer el archivo de la plantilla: " + e.getMessage());
        }
    }

    private void guardarArchivo(PlantillaDocumento plantilla, MultipartFile archivo) throws IOException {
        Path directorio = Paths.get(plantillasBasePath,
                "petrolera_" + plantilla.getPetroleraId(),
                plantilla.getModulo().name().toLowerCase(Locale.ROOT));
        Files.createDirectories(directorio);

        Path destino = directorio.resolve(UUID.randomUUID() + ".pdf");
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        plantilla.setRutaArchivo(destino.toString());
        plantilla.setNombreArchivo(archivo.getOriginalFilename());
    }

    private void eliminarArchivoDeDisco(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(ruta));
            log.info("Archivo PDF anterior eliminado: {}", ruta);
        } catch (IOException e) {
            log.error("No se pudo eliminar el archivo PDF anterior: {}", ruta, e);
        }
    }

    /**
     * Comprueba que el fichero subido es realmente un PDF: no basta con el content-type,
     * que lo fija el cliente, así que se valida también la firma de los primeros bytes.
     */
    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessValidationException("El archivo de la plantilla es obligatorio");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !"application/pdf".equalsIgnoreCase(contentType)) {
            throw new BusinessValidationException(
                    "El archivo de la plantilla debe ser un PDF (recibido: " + contentType + ")");
        }

        byte[] cabecera;
        try (InputStream input = archivo.getInputStream()) {
            cabecera = input.readNBytes(FIRMA_PDF.length);
        } catch (IOException e) {
            throw new BusinessValidationException(
                    "No se pudo leer el archivo de la plantilla: " + e.getMessage());
        }

        if (!Arrays.equals(cabecera, FIRMA_PDF)) {
            throw new BusinessValidationException("El archivo de la plantilla debe ser un PDF válido");
        }
    }

    private String normalizarTipoSolicitud(String tipoSolicitud) {
        if (tipoSolicitud == null || tipoSolicitud.isBlank()) {
            throw new BusinessValidationException("El tipo de solicitud es obligatorio");
        }
        return tipoSolicitud.trim().toUpperCase(Locale.ROOT);
    }

    private void validarPetroleraExiste(Long petroleraId) {
        petroleraRepository.findById(petroleraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Petrolera no encontrada con ID: " + petroleraId));
    }

    private PlantillaDocumentoDTO convertirADTO(PlantillaDocumento plantilla) {
        String petroleraNombre = petroleraRepository.findById(plantilla.getPetroleraId())
                .map(Petrolera::getNombre)
                .orElse(null);

        return PlantillaDocumentoDTO.builder()
                .id(plantilla.getId())
                .petroleraId(plantilla.getPetroleraId())
                .petroleraNombre(petroleraNombre)
                .modulo(plantilla.getModulo())
                .tipoSolicitud(plantilla.getTipoSolicitud())
                .nombreArchivo(plantilla.getNombreArchivo())
                .rutaArchivo(plantilla.getRutaArchivo())
                .activa(plantilla.getActiva())
                .createdAt(plantilla.getCreatedAt())
                .updatedAt(plantilla.getUpdatedAt())
                .build();
    }
}
