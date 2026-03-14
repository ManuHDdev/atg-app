package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.model.Tarjeta;
import com.manuhd.app.tarjetas.repository.TarjetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarjetaService {

    private final TarjetaRepository tarjetaRepository;

    @Transactional(readOnly = true)
    public List<Tarjeta> findAll() {
        log.info("Buscando todas las tarjetas");
        return tarjetaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tarjeta findById(Long id) {
        log.info("Buscando tarjeta con id: {}", id);
        return tarjetaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> findBySocioId(Long socioId) {
        log.info("Buscando tarjetas del socio: {}", socioId);
        return tarjetaRepository.findBySocioId(socioId);
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> findByPetroleraId(Long petroleraId) {
        log.info("Buscando tarjetas de la petrolera: {}", petroleraId);
        return tarjetaRepository.findByPetroleraId(petroleraId);
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> findAllActivas() {
        log.info("Buscando tarjetas activas");
        return tarjetaRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> findByMatricula(String matricula) {
        log.info("Buscando tarjetas con matrícula: {}", matricula);
        return tarjetaRepository.findByMatricula(matricula);
    }

    @Transactional
    public Tarjeta create(Tarjeta tarjeta) {
        log.info("Creando nueva tarjeta para matrícula: {}", tarjeta.getMatricula());

        // Validar datos obligatorios
        if (tarjeta.getSocioId() == null) {
            throw new RuntimeException("Debe especificar un socio para la tarjeta");
        }

        if (tarjeta.getPetroleraId() == null) {
            throw new RuntimeException("Debe especificar una petrolera para la tarjeta");
        }

        // Establecer fechaAlta automáticamente si no está presente
        if (tarjeta.getFechaAlta() == null) {
            tarjeta.setFechaAlta(LocalDateTime.now());
        }

        return tarjetaRepository.save(tarjeta);
    }

    @Transactional
    public Tarjeta update(Long id, Tarjeta tarjetaActualizada) {
        log.info("Actualizando tarjeta con id: {}", id);

        Tarjeta tarjeta = findById(id);

        tarjeta.setMatricula(tarjetaActualizada.getMatricula());
        tarjeta.setNumeroContrato(tarjetaActualizada.getNumeroContrato());
        tarjeta.setActiva(tarjetaActualizada.getActiva());
        tarjeta.setPetroleraId(tarjetaActualizada.getPetroleraId());

        return tarjetaRepository.save(tarjeta);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando tarjeta con id: {}", id);
        Tarjeta tarjeta = findById(id);
        tarjeta.setActiva(false);
        tarjeta.setDeletedAt(LocalDateTime.now());
        tarjetaRepository.save(tarjeta);
    }
}
