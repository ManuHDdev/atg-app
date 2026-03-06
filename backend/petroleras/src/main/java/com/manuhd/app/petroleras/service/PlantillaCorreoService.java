package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.dto.PlantillaCorreoDTO;
import com.manuhd.app.petroleras.exception.ResourceNotFoundException;
import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.model.PlantillaCorreo;
import com.manuhd.app.petroleras.repository.PlantillaCorreoRepository;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PlantillaCorreoService {

    @Autowired
    private PlantillaCorreoRepository plantillaCorreoRepository;

    @Autowired
    private PetroleraRepository petroleraRepository;

    @Transactional(readOnly = true)
    public List<PlantillaCorreoDTO> listarTodas() {
        return plantillaCorreoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlantillaCorreoDTO> listarPorPetrolera(Long petroleraId) {
        return plantillaCorreoRepository.findByPetroleraId(petroleraId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantillaCorreoDTO obtenerPorId(Long id) {
        PlantillaCorreo plantilla = plantillaCorreoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con id: " + id));
        return convertirADTO(plantilla);
    }

    @Transactional
    public PlantillaCorreoDTO crear(PlantillaCorreoDTO dto) {
        Petrolera petrolera = petroleraRepository.findById(dto.getPetroleraId())
                .orElseThrow(() -> new ResourceNotFoundException("Petrolera no encontrada con id: " + dto.getPetroleraId()));

        PlantillaCorreo plantilla = new PlantillaCorreo();
        plantilla.setPetrolera(petrolera);
        plantilla.setTipoPlantilla(dto.getTipoPlantilla());
        plantilla.setAsunto(dto.getAsunto());
        plantilla.setCuerpo(dto.getCuerpo());
        plantilla.setVariablesDisponibles(dto.getVariablesDisponibles());
        plantilla.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        PlantillaCorreo guardada = plantillaCorreoRepository.save(plantilla);
        log.info("Plantilla de correo creada con ID: {}", guardada.getId());

        return convertirADTO(guardada);
    }

    @Transactional
    public PlantillaCorreoDTO actualizar(Long id, PlantillaCorreoDTO dto) {
        PlantillaCorreo plantilla = plantillaCorreoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con id: " + id));

        if (dto.getPetroleraId() != null && !dto.getPetroleraId().equals(plantilla.getPetrolera().getId())) {
            Petrolera nuevaPetrolera = petroleraRepository.findById(dto.getPetroleraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Petrolera no encontrada con id: " + dto.getPetroleraId()));
            plantilla.setPetrolera(nuevaPetrolera);
        }

        if (dto.getTipoPlantilla() != null) {
            plantilla.setTipoPlantilla(dto.getTipoPlantilla());
        }
        if (dto.getAsunto() != null) {
            plantilla.setAsunto(dto.getAsunto());
        }
        if (dto.getCuerpo() != null) {
            plantilla.setCuerpo(dto.getCuerpo());
        }
        if (dto.getVariablesDisponibles() != null) {
            plantilla.setVariablesDisponibles(dto.getVariablesDisponibles());
        }
        if (dto.getActiva() != null) {
            plantilla.setActiva(dto.getActiva());
        }

        PlantillaCorreo actualizada = plantillaCorreoRepository.save(plantilla);
        log.info("Plantilla de correo actualizada con ID: {}", id);

        return convertirADTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!plantillaCorreoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plantilla no encontrada con id: " + id);
        }
        plantillaCorreoRepository.deleteById(id);
        log.info("Plantilla de correo eliminada con ID: {}", id);
    }

    public String procesarPlantilla(String cuerpo, java.util.Map<String, String> variables) {
        String resultado = cuerpo;
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            resultado = resultado.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resultado;
    }

    private PlantillaCorreoDTO convertirADTO(PlantillaCorreo plantilla) {
        PlantillaCorreoDTO dto = new PlantillaCorreoDTO();
        dto.setId(plantilla.getId());
        dto.setPetroleraId(plantilla.getPetrolera().getId());
        dto.setPetroleraNombre(plantilla.getPetrolera().getNombre());
        dto.setTipoPlantilla(plantilla.getTipoPlantilla());
        dto.setAsunto(plantilla.getAsunto());
        dto.setCuerpo(plantilla.getCuerpo());
        dto.setVariablesDisponibles(plantilla.getVariablesDisponibles());
        dto.setActiva(plantilla.getActiva());
        dto.setCreatedAt(plantilla.getCreatedAt());
        dto.setUpdatedAt(plantilla.getUpdatedAt());
        return dto;
    }
}
