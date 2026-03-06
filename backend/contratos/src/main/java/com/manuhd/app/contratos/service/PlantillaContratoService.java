package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.model.PlantillaContrato;
import com.manuhd.app.contratos.repository.PlantillaContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaContratoService {

    private final PlantillaContratoRepository plantillaRepository;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public List<PlantillaContrato> findAll() {
        log.info("Buscando todas las plantillas");
        return plantillaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlantillaContrato> findAllActivas() {
        log.info("Buscando plantillas activas");
        return plantillaRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public PlantillaContrato findById(Long id) {
        log.info("Buscando plantilla con id: {}", id);
        return plantillaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PlantillaContrato> findByPetroleraId(Long petroleraId) {
        log.info("Buscando plantillas de la petrolera: {}", petroleraId);
        return plantillaRepository.findByPetroleraId(petroleraId);
    }

    @Transactional(readOnly = true)
    public List<PlantillaContrato> findByPetroleraIdActivas(Long petroleraId) {
        log.info("Buscando plantillas activas de la petrolera: {}", petroleraId);
        return plantillaRepository.findByPetroleraIdAndActivaTrue(petroleraId);
    }

    @Transactional
    public PlantillaContrato create(PlantillaContrato plantilla, MultipartFile archivo) throws IOException {
        log.info("Creando nueva plantilla: {}", plantilla.getNombrePlantilla());

        if (archivo != null && !archivo.isEmpty()) {
            String rutaArchivo = pdfService.guardarPlantillaOrganizada(
                archivo,
                plantilla.getPetroleraId(),
                plantilla.getTipoContratoId(),
                plantilla.getTipoSolicitudPetroleraId()
            );
            plantilla.setRutaArchivo(rutaArchivo);
            plantilla.setNombreArchivoOriginal(archivo.getOriginalFilename());

            // Validar que el PDF es editable
            try {
                List<String> campos = pdfService.extraerCamposPdf(rutaArchivo);
                if (campos.isEmpty()) {
                    log.warn("El PDF no contiene campos editables");
                }
                log.info("Campos extraídos del PDF: {}", campos);
            } catch (Exception e) {
                log.error("Error al extraer campos del PDF", e);
            }
        }

        return plantillaRepository.save(plantilla);
    }

    @Transactional
    public PlantillaContrato update(Long id, PlantillaContrato plantillaActualizada) {
        log.info("Actualizando plantilla con id: {}", id);

        PlantillaContrato plantilla = findById(id);

        plantilla.setNombrePlantilla(plantillaActualizada.getNombrePlantilla());
        plantilla.setDescripcion(plantillaActualizada.getDescripcion());
        plantilla.setPetroleraId(plantillaActualizada.getPetroleraId());
        plantilla.setTipoContratoId(plantillaActualizada.getTipoContratoId());
        plantilla.setTipoSolicitudPetroleraId(plantillaActualizada.getTipoSolicitudPetroleraId());
        plantilla.setActiva(plantillaActualizada.getActiva());

        return plantillaRepository.save(plantilla);
    }

    @Transactional
    public PlantillaContrato actualizarArchivo(Long id, MultipartFile archivo) throws IOException {
        log.info("Actualizando archivo de plantilla con id: {}", id);

        PlantillaContrato plantilla = findById(id);

        String rutaArchivo = pdfService.guardarPlantilla(archivo);
        plantilla.setRutaArchivo(rutaArchivo);
        plantilla.setNombreArchivoOriginal(archivo.getOriginalFilename());
        plantilla.setVersion(plantilla.getVersion() + 1);

        return plantillaRepository.save(plantilla);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando plantilla con id: {}", id);
        PlantillaContrato plantilla = findById(id);
        plantilla.setActiva(false);
        plantillaRepository.save(plantilla);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerCamposPdf(Long id) throws IOException {
        log.info("Obteniendo campos del PDF de la plantilla: {}", id);
        PlantillaContrato plantilla = findById(id);

        if (plantilla.getRutaArchivo() == null || plantilla.getRutaArchivo().isEmpty()) {
            throw new RuntimeException("La plantilla no tiene archivo PDF asociado");
        }

        return pdfService.extraerCamposPdf(plantilla.getRutaArchivo());
    }

    @Transactional(readOnly = true)
    public PlantillaContrato obtenerPlantillaPorCriterios(Long petroleraId, Long tipoContratoId,
                                                          Long tipoSolicitudPetroleraId) {
        log.info("Buscando plantilla por criterios - Petrolera: {}, Tipo: {}, Subtipo: {}",
            petroleraId, tipoContratoId, tipoSolicitudPetroleraId);

        // Si hay tipoSolicitudPetroleraId, buscar plantilla específica
        if (tipoSolicitudPetroleraId != null) {
            return plantillaRepository
                .findByPetroleraIdAndTipoContratoIdAndTipoSolicitudPetroleraIdAndActivaTrue(
                    petroleraId, tipoContratoId, tipoSolicitudPetroleraId)
                .orElseGet(() -> {
                    log.info("No se encontró plantilla específica, buscando genérica");
                    return plantillaRepository
                        .findPlantillaGenerica(petroleraId, tipoContratoId)
                        .orElseThrow(() -> new RuntimeException(
                            "No se encontró plantilla para los criterios especificados"));
                });
        }

        // Si no hay tipoSolicitudPetroleraId, buscar plantilla genérica
        return plantillaRepository
            .findPlantillaGenerica(petroleraId, tipoContratoId)
            .orElseThrow(() -> new RuntimeException(
                "No se encontró plantilla para los criterios especificados"));
    }

    @Transactional(readOnly = true)
    public List<PlantillaContrato> findByPetroleraAndTipo(Long petroleraId, Long tipoContratoId) {
        log.info("Buscando plantillas de petrolera {} y tipo {}", petroleraId, tipoContratoId);
        return plantillaRepository.findByPetroleraIdAndTipoContratoIdAndActivaTrue(
            petroleraId, tipoContratoId);
    }

    @Transactional
    public void activar(Long id) {
        log.info("Activando plantilla con id: {}", id);
        PlantillaContrato plantilla = findById(id);
        plantilla.setActiva(true);
        plantillaRepository.save(plantilla);
    }

    @Transactional
    public void desactivar(Long id) {
        log.info("Desactivando plantilla con id: {}", id);
        PlantillaContrato plantilla = findById(id);
        plantilla.setActiva(false);
        plantillaRepository.save(plantilla);
    }
}
