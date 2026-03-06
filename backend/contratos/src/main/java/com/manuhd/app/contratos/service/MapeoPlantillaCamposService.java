package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.model.MapeoPlantillaCampos;
import com.manuhd.app.contratos.model.PlantillaContrato;
import com.manuhd.app.contratos.repository.MapeoPlantillaCamposRepository;
import com.manuhd.app.contratos.repository.PlantillaContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapeoPlantillaCamposService {

    private final MapeoPlantillaCamposRepository mapeoRepository;
    private final PlantillaContratoRepository plantillaRepository;

    @Transactional(readOnly = true)
    public List<MapeoPlantillaCampos> findAll() {
        log.info("Buscando todos los mapeos");
        return mapeoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public MapeoPlantillaCampos findById(Long id) {
        log.info("Buscando mapeo con id: {}", id);
        return mapeoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mapeo no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<MapeoPlantillaCampos> findByPlantillaId(Long plantillaId) {
        log.info("Buscando mapeos de la plantilla: {}", plantillaId);
        return mapeoRepository.findByPlantillaId(plantillaId);
    }

    @Transactional(readOnly = true)
    public List<MapeoPlantillaCampos> findByPlantillaIdActivos(Long plantillaId) {
        log.info("Buscando mapeos activos de la plantilla: {}", plantillaId);
        return mapeoRepository.findByPlantillaIdAndActivoTrue(plantillaId);
    }

    @Transactional
    public MapeoPlantillaCampos create(MapeoPlantillaCampos mapeo) {
        log.info("Creando nuevo mapeo para plantilla: {}", mapeo.getPlantilla().getId());

        // Validar que la plantilla existe
        if (mapeo.getPlantilla() == null || mapeo.getPlantilla().getId() == null) {
            throw new RuntimeException("Debe especificar una plantilla para el mapeo");
        }

        PlantillaContrato plantilla = plantillaRepository.findById(mapeo.getPlantilla().getId())
            .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + mapeo.getPlantilla().getId()));

        mapeo.setPlantilla(plantilla);

        return mapeoRepository.save(mapeo);
    }

    @Transactional
    public MapeoPlantillaCampos update(Long id, MapeoPlantillaCampos mapeoActualizado) {
        log.info("Actualizando mapeo con id: {}", id);

        MapeoPlantillaCampos mapeo = findById(id);

        mapeo.setNombreCampoPdf(mapeoActualizado.getNombreCampoPdf());
        mapeo.setTipoDato(mapeoActualizado.getTipoDato());
        mapeo.setCampoEntidad(mapeoActualizado.getCampoEntidad());
        mapeo.setActivo(mapeoActualizado.getActivo());

        return mapeoRepository.save(mapeo);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando mapeo con id: {}", id);
        MapeoPlantillaCampos mapeo = findById(id);
        mapeo.setActivo(false);
        mapeoRepository.save(mapeo);
    }

    @Transactional
    public void deletePhysically(Long id) {
        log.info("Eliminando físicamente mapeo con id: {}", id);
        mapeoRepository.deleteById(id);
    }
}
