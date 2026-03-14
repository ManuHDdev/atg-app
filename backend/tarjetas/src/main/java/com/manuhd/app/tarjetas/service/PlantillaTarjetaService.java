package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.dto.PlantillaTarjetaDTO;
import com.manuhd.app.tarjetas.model.PlantillaTarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.repository.PlantillaTarjetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlantillaTarjetaService {

    private final PlantillaTarjetaRepository repository;

    @Transactional(readOnly = true)
    public List<PlantillaTarjetaDTO> findAll() {
        log.info("Obteniendo todas las plantillas de tarjetas");
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantillaTarjetaDTO findById(Long id) {
        log.info("Buscando plantilla de tarjeta con id: {}", id);
        PlantillaTarjeta plantilla = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + id));
        return convertToDTO(plantilla);
    }

    @Transactional(readOnly = true)
    public PlantillaTarjetaDTO findByTipo(TipoPlantilla tipo) {
        log.info("Buscando plantilla de tipo: {}", tipo);
        PlantillaTarjeta plantilla = repository.findByTipo(tipo)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada para tipo: " + tipo));
        return convertToDTO(plantilla);
    }

    @Transactional(readOnly = true)
    public PlantillaTarjeta obtenerPlantillaActiva(TipoPlantilla tipo) {
        log.info("Obteniendo plantilla activa de tipo: {}", tipo);
        return repository.findByTipoAndActivaTrue(tipo)
                .orElseThrow(() -> new RuntimeException("No hay plantilla activa para tipo: " + tipo));
    }

    @Transactional
    public PlantillaTarjetaDTO create(PlantillaTarjetaDTO dto) {
        log.info("Creando nueva plantilla de tipo: {}", dto.getTipo());

        // Verificar si ya existe una plantilla para este tipo
        if (repository.findByTipo(dto.getTipo()).isPresent()) {
            throw new RuntimeException("Ya existe una plantilla para el tipo: " + dto.getTipo());
        }

        PlantillaTarjeta plantilla = convertToEntity(dto);
        PlantillaTarjeta saved = repository.save(plantilla);
        log.info("Plantilla creada con id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public PlantillaTarjetaDTO update(Long id, PlantillaTarjetaDTO dto) {
        log.info("Actualizando plantilla con id: {}", id);

        PlantillaTarjeta plantilla = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + id));

        plantilla.setAsunto(dto.getAsunto());
        plantilla.setCuerpo(dto.getCuerpo());
        plantilla.setVariablesDisponibles(dto.getVariablesDisponibles());
        plantilla.setActiva(dto.getActiva());

        PlantillaTarjeta updated = repository.save(plantilla);
        log.info("Plantilla actualizada con id: {}", updated.getId());
        return convertToDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando plantilla con id: {}", id);

        PlantillaTarjeta plantilla = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + id));
        plantilla.setActiva(false);
        plantilla.setDeletedAt(java.time.LocalDateTime.now());
        repository.save(plantilla);
        log.info("Plantilla eliminada (soft delete) con id: {}", id);
    }

    private PlantillaTarjetaDTO convertToDTO(PlantillaTarjeta entity) {
        PlantillaTarjetaDTO dto = new PlantillaTarjetaDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setAsunto(entity.getAsunto());
        dto.setCuerpo(entity.getCuerpo());
        dto.setVariablesDisponibles(entity.getVariablesDisponibles());
        dto.setActiva(entity.getActiva());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private PlantillaTarjeta convertToEntity(PlantillaTarjetaDTO dto) {
        PlantillaTarjeta entity = new PlantillaTarjeta();
        entity.setId(dto.getId());
        entity.setTipo(dto.getTipo());
        entity.setAsunto(dto.getAsunto());
        entity.setCuerpo(dto.getCuerpo());
        entity.setVariablesDisponibles(dto.getVariablesDisponibles());
        entity.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        return entity;
    }
}
