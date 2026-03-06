package com.manuhd.app.socios.service;

import com.manuhd.app.socios.dto.EmpresaDTO;
import com.manuhd.app.socios.model.Empresa;
import com.manuhd.app.socios.model.Socio;
import com.manuhd.app.socios.repository.EmpresaRepository;
import com.manuhd.app.socios.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final SocioRepository socioRepository;

    @Transactional(readOnly = true)
    public List<EmpresaDTO> findAll() {
        log.info("Buscando todas las empresas");
        return empresaRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpresaDTO findByIdDTO(Long id) {
        log.info("Buscando empresa con id: {}", id);
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
        return convertToDTO(empresa);
    }

    @Transactional(readOnly = true)
    public Empresa findById(Long id) {
        log.info("Buscando empresa con id: {}", id);
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<EmpresaDTO> findBySocioId(Long socioId) {
        log.info("Buscando empresas del socio: {}", socioId);
        return empresaRepository.findBySocioId(socioId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private EmpresaDTO convertToDTO(Empresa empresa) {
        EmpresaDTO dto = new EmpresaDTO();
        dto.setId(empresa.getId());
        dto.setSocioId(empresa.getSocio() != null ? empresa.getSocio().getId() : null);
        dto.setSocioNombre(empresa.getSocio() != null ? empresa.getSocio().getNombre() : null);
        dto.setNombre(empresa.getNombre());
        dto.setCif(empresa.getCif());
        dto.setDireccion(empresa.getDireccion());
        dto.setPoblacion(empresa.getPoblacion());
        dto.setProvincia(empresa.getProvincia());
        dto.setCodigoPostal(empresa.getCodigoPostal());
        dto.setEmail(empresa.getEmail());
        dto.setTelefono(empresa.getTelefono());
        dto.setFechaAlta(empresa.getFechaAlta());
        dto.setActiva(empresa.getActiva());
        dto.setCreatedAt(empresa.getCreatedAt());
        dto.setUpdatedAt(empresa.getUpdatedAt());
        return dto;
    }

    @Transactional
    public Empresa create(Empresa empresa) {
        log.info("Creando nueva empresa: {}", empresa.getNombre());

        // Validar que el socio existe
        if (empresa.getSocio() == null || empresa.getSocio().getId() == null) {
            throw new RuntimeException("Debe especificar un socio para la empresa");
        }

        socioRepository.findById(empresa.getSocio().getId())
            .orElseThrow(() -> new RuntimeException("Socio no encontrado con id: " + empresa.getSocio().getId()));

        // Establecer fechaAlta automáticamente si no está presente
        if (empresa.getFechaAlta() == null) {
            empresa.setFechaAlta(LocalDateTime.now());
        }

        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa update(Long id, Empresa empresaActualizada) {
        log.info("Actualizando empresa con id: {}", id);

        Empresa empresa = findById(id);

        empresa.setNombre(empresaActualizada.getNombre());
        empresa.setCif(empresaActualizada.getCif());
        empresa.setDireccion(empresaActualizada.getDireccion());
        empresa.setPoblacion(empresaActualizada.getPoblacion());
        empresa.setProvincia(empresaActualizada.getProvincia());
        empresa.setCodigoPostal(empresaActualizada.getCodigoPostal());
        empresa.setEmail(empresaActualizada.getEmail());
        empresa.setTelefono(empresaActualizada.getTelefono());
        empresa.setActiva(empresaActualizada.getActiva());

        return empresaRepository.save(empresa);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando empresa con id: {}", id);
        empresaRepository.deleteById(id);
    }

    @Transactional
    public EmpresaDTO createFromDTO(EmpresaDTO empresaDTO) {
        log.info("Creando nueva empresa desde DTO: {}", empresaDTO.getNombre());

        // Validar que se proporciona un socioId
        if (empresaDTO.getSocioId() == null) {
            throw new RuntimeException("Debe especificar un socio para la empresa");
        }

        // Buscar el socio
        Socio socio = socioRepository.findById(empresaDTO.getSocioId())
            .orElseThrow(() -> new RuntimeException("Socio no encontrado con id: " + empresaDTO.getSocioId()));

        // Crear la empresa
        Empresa empresa = new Empresa();
        empresa.setSocio(socio);
        empresa.setNombre(empresaDTO.getNombre());
        empresa.setCif(empresaDTO.getCif());
        empresa.setDireccion(empresaDTO.getDireccion());
        empresa.setPoblacion(empresaDTO.getPoblacion());
        empresa.setProvincia(empresaDTO.getProvincia());
        empresa.setCodigoPostal(empresaDTO.getCodigoPostal());
        empresa.setEmail(empresaDTO.getEmail());
        empresa.setTelefono(empresaDTO.getTelefono());
        empresa.setActiva(empresaDTO.getActiva() != null ? empresaDTO.getActiva() : true);

        // Establecer fechaAlta automáticamente
        empresa.setFechaAlta(LocalDateTime.now());

        Empresa saved = empresaRepository.save(empresa);
        return convertToDTO(saved);
    }

    @Transactional
    public EmpresaDTO updateFromDTO(Long id, EmpresaDTO empresaDTO) {
        log.info("Actualizando empresa con id: {} desde DTO", id);

        Empresa empresa = findById(id);

        // Si se proporciona un nuevo socioId, actualizar el socio
        if (empresaDTO.getSocioId() != null && !empresaDTO.getSocioId().equals(empresa.getSocio().getId())) {
            Socio nuevoSocio = socioRepository.findById(empresaDTO.getSocioId())
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con id: " + empresaDTO.getSocioId()));
            empresa.setSocio(nuevoSocio);
        }

        empresa.setNombre(empresaDTO.getNombre());
        empresa.setCif(empresaDTO.getCif());
        empresa.setDireccion(empresaDTO.getDireccion());
        empresa.setPoblacion(empresaDTO.getPoblacion());
        empresa.setProvincia(empresaDTO.getProvincia());
        empresa.setCodigoPostal(empresaDTO.getCodigoPostal());
        empresa.setEmail(empresaDTO.getEmail());
        empresa.setTelefono(empresaDTO.getTelefono());
        empresa.setActiva(empresaDTO.getActiva());

        // fechaAlta NO se actualiza - es inmutable

        Empresa updated = empresaRepository.save(empresa);
        return convertToDTO(updated);
    }
}
