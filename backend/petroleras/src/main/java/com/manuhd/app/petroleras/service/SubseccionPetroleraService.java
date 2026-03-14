package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.dto.SubseccionPetroleraDTO;
import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.model.SubseccionPetrolera;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import com.manuhd.app.petroleras.repository.SubseccionPetroleraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubseccionPetroleraService {

    private final SubseccionPetroleraRepository subseccionRepository;
    private final PetroleraRepository petroleraRepository;

    @Transactional(readOnly = true)
    public List<SubseccionPetroleraDTO> findAll() {
        log.info("Buscando todas las subsecciones");
        return subseccionRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubseccionPetroleraDTO findById(Long id) {
        log.info("Buscando subsección con id: {}", id);
        SubseccionPetrolera subseccion = subseccionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Subsección no encontrada con id: " + id));
        return convertToDTO(subseccion);
    }

    @Transactional(readOnly = true)
    public List<SubseccionPetroleraDTO> findByPetroleraId(Long petroleraId) {
        log.info("Buscando subsecciones de la petrolera: {}", petroleraId);
        return subseccionRepository.findByPetroleraIdOrderByOrdenAsc(petroleraId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubseccionPetroleraDTO> findActivasByPetroleraId(Long petroleraId) {
        log.info("Buscando subsecciones activas de la petrolera: {}", petroleraId);
        return subseccionRepository.findByPetroleraIdAndActivaTrueOrderByOrdenAsc(petroleraId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public SubseccionPetroleraDTO create(SubseccionPetroleraDTO dto) {
        log.info("Creando nueva subsección: {}", dto.getNombre());

        if (dto.getPetroleraId() == null) {
            throw new RuntimeException("Debe especificar una petrolera para la subsección");
        }

        Petrolera petrolera = petroleraRepository.findById(dto.getPetroleraId())
            .orElseThrow(() -> new RuntimeException("Petrolera no encontrada con id: " + dto.getPetroleraId()));

        SubseccionPetrolera subseccion = new SubseccionPetrolera();
        subseccion.setPetrolera(petrolera);
        subseccion.setNombre(dto.getNombre());
        subseccion.setCodigo(dto.getCodigo());
        subseccion.setOrden(dto.getOrden() != null ? dto.getOrden() : 0);
        subseccion.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        SubseccionPetrolera saved = subseccionRepository.save(subseccion);
        return convertToDTO(saved);
    }

    @Transactional
    public SubseccionPetroleraDTO update(Long id, SubseccionPetroleraDTO dto) {
        log.info("Actualizando subsección con id: {}", id);

        SubseccionPetrolera subseccion = subseccionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Subsección no encontrada con id: " + id));

        subseccion.setNombre(dto.getNombre());
        subseccion.setCodigo(dto.getCodigo());
        subseccion.setOrden(dto.getOrden());
        subseccion.setActiva(dto.getActiva());

        SubseccionPetrolera updated = subseccionRepository.save(subseccion);
        return convertToDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando subsección con id: {}", id);
        SubseccionPetrolera subseccion = subseccionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Subsección no encontrada con id: " + id));
        subseccion.setActiva(false);
        subseccion.setDeletedAt(java.time.LocalDateTime.now());
        subseccionRepository.save(subseccion);
    }

    private SubseccionPetroleraDTO convertToDTO(SubseccionPetrolera subseccion) {
        SubseccionPetroleraDTO dto = new SubseccionPetroleraDTO();
        dto.setId(subseccion.getId());
        dto.setPetroleraId(subseccion.getPetrolera() != null ? subseccion.getPetrolera().getId() : null);
        dto.setPetroleraNombre(subseccion.getPetrolera() != null ? subseccion.getPetrolera().getNombre() : null);
        dto.setNombre(subseccion.getNombre());
        dto.setCodigo(subseccion.getCodigo());
        dto.setOrden(subseccion.getOrden());
        dto.setActiva(subseccion.getActiva());
        dto.setCreatedAt(subseccion.getCreatedAt());
        dto.setUpdatedAt(subseccion.getUpdatedAt());
        return dto;
    }
}
