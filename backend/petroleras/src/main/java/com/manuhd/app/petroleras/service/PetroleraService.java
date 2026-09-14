package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetroleraService {

    private final PetroleraRepository petroleraRepository;

    @Transactional(readOnly = true)
    public List<Petrolera> findAll() {
        log.info("Buscando todas las petroleras");
        return petroleraRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Petrolera> findAllActivas() {
        log.info("Buscando petroleras activas");
        return petroleraRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public Petrolera findById(Long id) {
        log.info("Buscando petrolera con id: {}", id);
        return petroleraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Petrolera no encontrada con id: " + id));
    }

    @Transactional
    public Petrolera create(Petrolera petrolera) {
        log.info("Creando nueva petrolera: {}", petrolera.getNombre());

        // Validar que el nombre es único
        petroleraRepository.findByNombre(petrolera.getNombre())
            .ifPresent(p -> {
                throw new RuntimeException("Ya existe una petrolera con el nombre: " + petrolera.getNombre());
            });

        return petroleraRepository.save(petrolera);
    }

    @Transactional
    public Petrolera update(Long id, Petrolera petroleraActualizada) {
        log.info("Actualizando petrolera con id: {}", id);

        Petrolera petrolera = findById(id);

        // Validar nombre único si cambió
        if (!petrolera.getNombre().equals(petroleraActualizada.getNombre())) {
            petroleraRepository.findByNombre(petroleraActualizada.getNombre())
                .ifPresent(p -> {
                    throw new RuntimeException("Ya existe una petrolera con el nombre: " + petroleraActualizada.getNombre());
                });
        }

        petrolera.setNombre(petroleraActualizada.getNombre());
        petrolera.setActiva(petroleraActualizada.getActiva());
        petrolera.setEmail(petroleraActualizada.getEmail());
        petrolera.setDiasEnvioCreditos(petroleraActualizada.getDiasEnvioCreditos());
        petrolera.setOperaTarjetas(petroleraActualizada.getOperaTarjetas());
        petrolera.setOperaContratos(petroleraActualizada.getOperaContratos());
        petrolera.setOperaCreditos(petroleraActualizada.getOperaCreditos());
        petrolera.setOperaDispositivos(petroleraActualizada.getOperaDispositivos());
        petrolera.setPermiteCreditoDispositivo(petroleraActualizada.getPermiteCreditoDispositivo());

        return petroleraRepository.save(petrolera);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando petrolera con id: {}", id);
        Petrolera petrolera = findById(id);
        petrolera.setActiva(false);
        petroleraRepository.save(petrolera);
    }
}
