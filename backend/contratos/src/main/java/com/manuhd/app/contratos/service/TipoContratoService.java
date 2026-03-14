package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.dto.TipoContratoDTO;
import com.manuhd.app.contratos.exception.DuplicateResourceException;
import com.manuhd.app.contratos.exception.ResourceNotFoundException;
import com.manuhd.app.contratos.model.TipoContrato;
import com.manuhd.app.contratos.repository.TipoContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoContratoService {

    private final TipoContratoRepository tipoContratoRepository;

    @Transactional(readOnly = true)
    public List<TipoContratoDTO> listarTodos() {
        log.info("Listando todos los tipos de contrato");
        return tipoContratoRepository.findAll().stream()
            .map(TipoContratoDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoContratoDTO> listarActivos() {
        log.info("Listando tipos de contrato activos");
        return tipoContratoRepository.findByActivoTrue().stream()
            .map(TipoContratoDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoContratoDTO obtenerPorId(Long id) {
        log.info("Obteniendo tipo de contrato por ID: {}", id);
        TipoContrato tipoContrato = tipoContratoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato", "ID", id));
        return TipoContratoDTO.fromEntity(tipoContrato);
    }

    @Transactional(readOnly = true)
    public TipoContratoDTO obtenerPorCodigo(String codigo) {
        log.info("Obteniendo tipo de contrato por código: {}", codigo);
        TipoContrato tipoContrato = tipoContratoRepository.findByCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato", "código", codigo));
        return TipoContratoDTO.fromEntity(tipoContrato);
    }

    @Transactional
    public TipoContratoDTO crear(TipoContratoDTO dto) {
        log.info("Creando nuevo tipo de contrato: {}", dto.getCodigo());

        if (tipoContratoRepository.existsByCodigo(dto.getCodigo())) {
            throw new DuplicateResourceException("Tipo de contrato", "código", dto.getCodigo());
        }

        TipoContrato tipoContrato = dto.toEntity();
        TipoContrato saved = tipoContratoRepository.save(tipoContrato);

        log.info("Tipo de contrato creado exitosamente con ID: {}", saved.getId());
        return TipoContratoDTO.fromEntity(saved);
    }

    @Transactional
    public TipoContratoDTO actualizar(Long id, TipoContratoDTO dto) {
        log.info("Actualizando tipo de contrato con ID: {}", id);

        TipoContrato existente = tipoContratoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato", "ID", id));

        if (!existente.getCodigo().equals(dto.getCodigo()) &&
            tipoContratoRepository.existsByCodigo(dto.getCodigo())) {
            throw new DuplicateResourceException("Tipo de contrato", "código", dto.getCodigo());
        }

        existente.setCodigo(dto.getCodigo());
        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        existente.setActivo(dto.getActivo());

        TipoContrato updated = tipoContratoRepository.save(existente);
        log.info("Tipo de contrato actualizado exitosamente");
        return TipoContratoDTO.fromEntity(updated);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando tipo de contrato con ID: {}", id);

        TipoContrato tipoContrato = tipoContratoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato", "ID", id));

        tipoContrato.setActivo(false);
        tipoContrato.setDeletedAt(java.time.LocalDateTime.now());
        tipoContratoRepository.save(tipoContrato);
        log.info("Tipo de contrato eliminado (soft delete) exitosamente");
    }

    @Transactional
    public void activar(Long id) {
        log.info("Activando tipo de contrato con ID: {}", id);
        cambiarEstado(id, true);
    }

    @Transactional
    public void desactivar(Long id) {
        log.info("Desactivando tipo de contrato con ID: {}", id);
        cambiarEstado(id, false);
    }

    private void cambiarEstado(Long id, boolean activo) {
        TipoContrato tipoContrato = tipoContratoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de contrato", "ID", id));

        tipoContrato.setActivo(activo);
        tipoContratoRepository.save(tipoContrato);
        log.info("Estado de tipo de contrato cambiado a: {}", activo);
    }
}
