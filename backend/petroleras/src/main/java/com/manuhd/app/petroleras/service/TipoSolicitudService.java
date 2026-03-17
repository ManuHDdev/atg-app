package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.dto.TipoSolicitudDTO;
import com.manuhd.app.petroleras.exception.ResourceNotFoundException;
import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.model.TipoSolicitud;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import com.manuhd.app.petroleras.repository.TipoSolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoSolicitudService {

    private final TipoSolicitudRepository tipoSolicitudRepository;
    private final PetroleraRepository petroleraRepository;

    @Value("${app.storage.plantillas-path:storage/plantillas}")
    private String plantillasBasePath;

    @Transactional(readOnly = true)
    public List<TipoSolicitudDTO> listarTodos() {
        log.info("Listando todos los tipos de solicitud");
        return tipoSolicitudRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoSolicitudDTO> listarPorPetrolera(Long petroleraId) {
        log.info("Listando tipos de solicitud para petrolera ID: {}", petroleraId);
        return tipoSolicitudRepository.findByPetroleraIdOrderByOrdenAsc(petroleraId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoSolicitudDTO> listarActivasPorPetrolera(Long petroleraId) {
        log.info("Listando tipos de solicitud activas para petrolera ID: {}", petroleraId);
        return tipoSolicitudRepository.findByPetroleraIdAndActiva(petroleraId, true).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoSolicitudDTO obtenerPorId(Long id) {
        log.info("Obteniendo tipo de solicitud ID: {}", id);
        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));
        return convertirADTO(tipoSolicitud);
    }

    @Transactional
    public TipoSolicitudDTO crear(TipoSolicitudDTO dto) {
        log.info("Creando nuevo tipo de solicitud: {}", dto.getNombre());

        // Verificar que la petrolera existe
        petroleraRepository.findById(dto.getPetroleraId())
                .orElseThrow(() -> new ResourceNotFoundException("Petrolera no encontrada con ID: " + dto.getPetroleraId()));

        TipoSolicitud tipoSolicitud = TipoSolicitud.builder()
                .petroleraId(dto.getPetroleraId())
                .nombre(dto.getNombre())
                .codigo(dto.getCodigo())
                .descripcion(dto.getDescripcion())
                .orden(dto.getOrden())
                .activa(dto.getActiva())
                .build();

        TipoSolicitud guardado = tipoSolicitudRepository.save(tipoSolicitud);
        log.info("Tipo de solicitud creado con ID: {}", guardado.getId());
        return convertirADTO(guardado);
    }

    @Transactional
    public TipoSolicitudDTO actualizar(Long id, TipoSolicitudDTO dto) {
        log.info("Actualizando tipo de solicitud ID: {}", id);

        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));

        // Verificar que la petrolera existe si se está cambiando
        if (!tipoSolicitud.getPetroleraId().equals(dto.getPetroleraId())) {
            petroleraRepository.findById(dto.getPetroleraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Petrolera no encontrada con ID: " + dto.getPetroleraId()));
        }

        tipoSolicitud.setPetroleraId(dto.getPetroleraId());
        tipoSolicitud.setNombre(dto.getNombre());
        tipoSolicitud.setCodigo(dto.getCodigo());
        tipoSolicitud.setDescripcion(dto.getDescripcion());
        if (dto.getOrden() != null) {
            tipoSolicitud.setOrden(dto.getOrden());
        }
        tipoSolicitud.setActiva(dto.getActiva());

        TipoSolicitud actualizado = tipoSolicitudRepository.save(tipoSolicitud);
        log.info("Tipo de solicitud actualizado ID: {}", id);
        return convertirADTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando tipo de solicitud ID: {}", id);

        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));

        tipoSolicitud.setActiva(false);
        tipoSolicitud.setDeletedAt(java.time.LocalDateTime.now());
        tipoSolicitudRepository.save(tipoSolicitud);
        log.info("Tipo de solicitud eliminado (soft delete) ID: {}", id);
    }

    @Transactional
    public TipoSolicitudDTO subirPlantillaPdf(Long id, MultipartFile archivo) throws IOException {
        log.info("Subiendo plantilla PDF para tipo de solicitud ID: {}", id);

        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));

        // Eliminar PDF anterior si existe
        if (tipoSolicitud.getRutaPlantillaPdf() != null) {
            eliminarArchivoPdf(tipoSolicitud.getRutaPlantillaPdf());
        }

        // Generar nombre único para el archivo
        String nombreOriginal = archivo.getOriginalFilename();
        String extension = nombreOriginal != null && nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf("."))
                : ".pdf";
        String nombreUnico = UUID.randomUUID().toString() + extension;

        // Crear directorio si no existe
        Path directorioPlantillas = Paths.get(plantillasBasePath, "petrolera_" + tipoSolicitud.getPetroleraId());
        Files.createDirectories(directorioPlantillas);

        // Guardar archivo
        Path rutaArchivo = directorioPlantillas.resolve(nombreUnico);
        Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

        // Actualizar entidad
        tipoSolicitud.setRutaPlantillaPdf(rutaArchivo.toString());
        tipoSolicitud.setNombreArchivoPlantilla(nombreOriginal);

        TipoSolicitud actualizado = tipoSolicitudRepository.save(tipoSolicitud);
        log.info("Plantilla PDF guardada para tipo de solicitud ID: {}", id);
        return convertirADTO(actualizado);
    }

    @Transactional
    public void eliminarPlantillaPdf(Long id) {
        log.info("Eliminando plantilla PDF de tipo de solicitud ID: {}", id);

        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));

        if (tipoSolicitud.getRutaPlantillaPdf() != null) {
            eliminarArchivoPdf(tipoSolicitud.getRutaPlantillaPdf());
            tipoSolicitud.setRutaPlantillaPdf(null);
            tipoSolicitud.setNombreArchivoPlantilla(null);
            tipoSolicitudRepository.save(tipoSolicitud);
            log.info("Plantilla PDF eliminada de tipo de solicitud ID: {}", id);
        }
    }

    @Transactional
    public byte[] descargarPlantillaPdf(Long id) throws IOException {
        log.info("Descargando plantilla PDF de tipo de solicitud ID: {}", id);

        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de solicitud no encontrado con ID: " + id));

        if (tipoSolicitud.getRutaPlantillaPdf() == null) {
            throw new ResourceNotFoundException("El tipo de solicitud no tiene plantilla PDF configurada");
        }

        Path path = Paths.get(tipoSolicitud.getRutaPlantillaPdf());
        if (!Files.exists(path)) {
            log.warn("Archivo PDF no encontrado en disco, limpiando referencia para ID {}: {}", id, tipoSolicitud.getRutaPlantillaPdf());
            tipoSolicitud.setRutaPlantillaPdf(null);
            tipoSolicitud.setNombreArchivoPlantilla(null);
            tipoSolicitudRepository.save(tipoSolicitud);
            throw new ResourceNotFoundException("La plantilla PDF de este tipo de solicitud no está disponible. Por favor, vuelva a subirla.");
        }

        return Files.readAllBytes(path);
    }

    private void eliminarArchivoPdf(String ruta) {
        try {
            Path path = Paths.get(ruta);
            Files.deleteIfExists(path);
            log.info("Archivo PDF eliminado: {}", ruta);
        } catch (IOException e) {
            log.error("Error al eliminar archivo PDF: {}", ruta, e);
        }
    }

    private TipoSolicitudDTO convertirADTO(TipoSolicitud tipoSolicitud) {
        String petroleraNombre = petroleraRepository.findById(tipoSolicitud.getPetroleraId())
                .map(Petrolera::getNombre)
                .orElse(null);

        return TipoSolicitudDTO.builder()
                .id(tipoSolicitud.getId())
                .petroleraId(tipoSolicitud.getPetroleraId())
                .petroleraNombre(petroleraNombre)
                .nombre(tipoSolicitud.getNombre())
                .codigo(tipoSolicitud.getCodigo())
                .descripcion(tipoSolicitud.getDescripcion())
                .orden(tipoSolicitud.getOrden())
                .activa(tipoSolicitud.getActiva())
                .rutaPlantillaPdf(tipoSolicitud.getRutaPlantillaPdf())
                .nombreArchivoPlantilla(tipoSolicitud.getNombreArchivoPlantilla())
                .createdAt(tipoSolicitud.getCreatedAt())
                .updatedAt(tipoSolicitud.getUpdatedAt())
                .build();
    }
}
