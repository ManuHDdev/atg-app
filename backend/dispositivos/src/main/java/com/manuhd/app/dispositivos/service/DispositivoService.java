package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.dto.DispositivoDTO;
import com.manuhd.app.dispositivos.model.Dispositivo;
import com.manuhd.app.dispositivos.repository.DispositivoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DispositivoService {

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Las URLs de los microservicios se configuran por entorno: en produccion apuntan a los
    // nombres de servicio de Docker, no a localhost. Fijarlas en codigo rompia silenciosamente
    // el enriquecimiento de datos del dispositivo fuera del entorno local.
    @Value("${microservices.socios.url:http://localhost:8081}")
    private String sociosBaseUrl;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    @Transactional(readOnly = true)
    public List<DispositivoDTO> listarPorSocio(Long socioId) {
        return dispositivoRepository.findBySocioId(socioId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DispositivoDTO> listarActivosPorSocio(Long socioId) {
        return dispositivoRepository.findBySocioIdAndActivoTrue(socioId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DispositivoDTO obtenerPorId(Long id) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado con id: " + id));
        return convertirADTO(dispositivo);
    }

    @Transactional
    public Dispositivo crearDispositivo(Long socioId, Long petroleraId, String matricula, Long solicitudAltaId) {
        log.info("Creando dispositivo para socio {} con matricula {}", socioId, matricula);

        // Validar que la matricula no tenga ya un dispositivo activo
        Optional<Dispositivo> existente = dispositivoRepository.findByMatriculaAndActivoTrue(matricula);
        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe un dispositivo activo con la matricula: " + matricula);
        }

        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setSocioId(socioId);
        dispositivo.setPetroleraId(petroleraId);
        dispositivo.setMatricula(matricula.toUpperCase().trim());
        dispositivo.setSolicitudAltaId(solicitudAltaId);
        dispositivo.setFechaAlta(LocalDateTime.now());
        dispositivo.setActivo(true);

        return dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public Dispositivo darDeBaja(Long dispositivoId) {
        log.info("Dando de baja dispositivo {}", dispositivoId);

        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado con id: " + dispositivoId));

        if (!dispositivo.getActivo()) {
            throw new RuntimeException("El dispositivo ya esta dado de baja");
        }

        dispositivo.setActivo(false);
        dispositivo.setFechaBaja(LocalDateTime.now());

        return dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public Dispositivo cambiarMatricula(Long dispositivoId, String nuevaMatricula) {
        log.info("Cambiando matricula del dispositivo {} a {}", dispositivoId, nuevaMatricula);

        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado con id: " + dispositivoId));

        if (!dispositivo.getActivo()) {
            throw new RuntimeException("El dispositivo no esta activo");
        }

        // Validar que la nueva matricula no tenga dispositivo activo
        Optional<Dispositivo> existente = dispositivoRepository.findByMatriculaAndActivoTrue(nuevaMatricula);
        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe un dispositivo activo con la matricula: " + nuevaMatricula);
        }

        dispositivo.setMatricula(nuevaMatricula.toUpperCase().trim());

        return dispositivoRepository.save(dispositivo);
    }

    public boolean validarMatriculaDisponible(String matricula) {
        return dispositivoRepository.findByMatriculaAndActivoTrue(matricula).isEmpty();
    }

    private DispositivoDTO convertirADTO(Dispositivo dispositivo) {
        DispositivoDTO dto = new DispositivoDTO();
        dto.setId(dispositivo.getId());
        dto.setSocioId(dispositivo.getSocioId());
        dto.setPetroleraId(dispositivo.getPetroleraId());
        dto.setMatricula(dispositivo.getMatricula());
        dto.setActivo(dispositivo.getActivo());
        dto.setFechaAlta(dispositivo.getFechaAlta());
        dto.setFechaBaja(dispositivo.getFechaBaja());
        dto.setCreatedAt(dispositivo.getCreatedAt());

        // Enrichment
        try {
            Map<String, Object> socio = restTemplate.getForObject(sociosBaseUrl + "/api/socios/" + dispositivo.getSocioId(), Map.class);
            if (socio != null) {
                dto.setSocioNombre((String) socio.get("nombre"));
            }
        } catch (Exception e) {
            log.warn("Error al obtener datos del socio: {}", e.getMessage());
        }

        try {
            Map<String, Object> petrolera = restTemplate.getForObject(petrolerasBaseUrl + "/api/petroleras/" + dispositivo.getPetroleraId(), Map.class);
            if (petrolera != null) {
                dto.setPetroleraNombre((String) petrolera.get("nombre"));
            }
        } catch (Exception e) {
            log.warn("Error al obtener datos de la petrolera: {}", e.getMessage());
        }

        return dto;
    }
}
