package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.dto.TipoSolicitudPetroleraDTO;
import com.manuhd.app.contratos.model.TipoSolicitudPetrolera;
import com.manuhd.app.contratos.repository.TipoSolicitudPetroleraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoSolicitudPetroleraService {

    private final TipoSolicitudPetroleraRepository tipoSolicitudPetroleraRepository;

    @Transactional(readOnly = true)
    public List<TipoSolicitudPetroleraDTO> listarTodos() {
        log.info("Listando todos los tipos de solicitud petrolera");
        return tipoSolicitudPetroleraRepository.findAll().stream()
            .map(TipoSolicitudPetroleraDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoSolicitudPetroleraDTO> listarPorPetrolera(Long petroleraId) {
        log.info("Listando tipos de solicitud para petrolera: {}", petroleraId);
        return tipoSolicitudPetroleraRepository.findByPetroleraIdOrderByOrdenAsc(petroleraId).stream()
            .map(TipoSolicitudPetroleraDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoSolicitudPetroleraDTO> listarPorPetroleraYTipo(Long petroleraId, Long tipoContratoId) {
        log.info("Listando tipos de solicitud para petrolera {} y tipo contrato {}", petroleraId, tipoContratoId);
        return tipoSolicitudPetroleraRepository
            .findByPetroleraIdAndTipoContratoId(petroleraId, tipoContratoId).stream()
            .map(TipoSolicitudPetroleraDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoSolicitudPetroleraDTO> listarActivasPorPetroleraYTipo(Long petroleraId, Long tipoContratoId) {
        log.info("Listando tipos de solicitud activas para petrolera {} y tipo contrato {}",
            petroleraId, tipoContratoId);
        return tipoSolicitudPetroleraRepository
            .findByPetroleraIdAndTipoContratoIdAndActivoTrue(petroleraId, tipoContratoId).stream()
            .map(TipoSolicitudPetroleraDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoSolicitudPetroleraDTO obtenerPorId(Long id) {
        log.info("Obteniendo tipo de solicitud petrolera por ID: {}", id);
        TipoSolicitudPetrolera tipo = tipoSolicitudPetroleraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de solicitud petrolera no encontrado con ID: " + id));
        return TipoSolicitudPetroleraDTO.fromEntity(tipo);
    }

    @Transactional
    public TipoSolicitudPetroleraDTO crear(TipoSolicitudPetroleraDTO dto) {
        log.info("Creando nuevo tipo de solicitud petrolera: {}", dto.getNombre());

        TipoSolicitudPetrolera tipo = dto.toEntity();
        TipoSolicitudPetrolera saved = tipoSolicitudPetroleraRepository.save(tipo);

        log.info("Tipo de solicitud petrolera creado exitosamente con ID: {}", saved.getId());
        return TipoSolicitudPetroleraDTO.fromEntity(saved);
    }

    @Transactional
    public TipoSolicitudPetroleraDTO actualizar(Long id, TipoSolicitudPetroleraDTO dto) {
        log.info("Actualizando tipo de solicitud petrolera con ID: {}", id);

        TipoSolicitudPetrolera existente = tipoSolicitudPetroleraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de solicitud petrolera no encontrado con ID: " + id));

        existente.setPetroleraId(dto.getPetroleraId());
        existente.setTipoContratoId(dto.getTipoContratoId());
        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        existente.setOrden(dto.getOrden());
        existente.setActivo(dto.getActivo());

        TipoSolicitudPetrolera updated = tipoSolicitudPetroleraRepository.save(existente);
        log.info("Tipo de solicitud petrolera actualizado exitosamente");
        return TipoSolicitudPetroleraDTO.fromEntity(updated);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando tipo de solicitud petrolera con ID: {}", id);

        TipoSolicitudPetrolera tipo = tipoSolicitudPetroleraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de solicitud petrolera no encontrado con ID: " + id));

        tipo.setActivo(false);
        tipo.setDeletedAt(java.time.LocalDateTime.now());
        tipoSolicitudPetroleraRepository.save(tipo);
        log.info("Tipo de solicitud petrolera eliminado (soft delete) exitosamente");
    }

    @Transactional
    public void activar(Long id) {
        log.info("Activando tipo de solicitud petrolera con ID: {}", id);
        cambiarEstado(id, true);
    }

    @Transactional
    public void desactivar(Long id) {
        log.info("Desactivando tipo de solicitud petrolera con ID: {}", id);
        cambiarEstado(id, false);
    }

    private void cambiarEstado(Long id, boolean activo) {
        TipoSolicitudPetrolera tipo = tipoSolicitudPetroleraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de solicitud petrolera no encontrado con ID: " + id));

        tipo.setActivo(activo);
        tipoSolicitudPetroleraRepository.save(tipo);
        log.info("Estado de tipo de solicitud petrolera cambiado a: {}", activo);
    }
}
