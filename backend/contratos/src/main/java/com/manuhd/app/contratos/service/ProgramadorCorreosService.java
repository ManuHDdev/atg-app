package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.model.Credito;
import com.manuhd.app.contratos.repository.CreditoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ProgramadorCorreosService {

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private CreditoService creditoService;

    /**
     * Tarea programada que se ejecuta todos los días a las 9:00 AM
     * Verifica créditos programados para envío
     */
    @Scheduled(cron = "0 0 9 * * *") // Todos los días a las 9:00 AM
    @Transactional
    public void procesarCreditosProgramados() {
        log.info("Iniciando procesamiento de créditos programados");

        LocalDate hoy = LocalDate.now();
        List<Credito> creditosProgramados = creditoRepository
                .findByProgramadoEnvioTrueAndFechaProgramadaEnvio(hoy);

        log.info("Encontrados {} créditos programados para hoy", creditosProgramados.size());

        for (Credito credito : creditosProgramados) {
            try {
                log.info("Enviando crédito programado ID: {}", credito.getId());
                creditoService.enviarAPetrolera(credito.getId());
                log.info("Crédito {} enviado exitosamente", credito.getId());
            } catch (Exception e) {
                log.error("Error al enviar crédito programado {}: {}", credito.getId(), e.getMessage());
            }
        }

        log.info("Finalizado procesamiento de créditos programados");
    }

    /**
     * Tarea programada para envíos específicos por día de la semana
     * Por ejemplo: Repsol solo envía los jueves
     */
    @Scheduled(cron = "0 0 9 * * THU") // Todos los jueves a las 9:00 AM
    @Transactional
    public void procesarCreditosJueves() {
        log.info("Procesando créditos específicos para jueves");

        // Aquí se pueden agregar lógicas específicas por petrolera
        // Por ejemplo, buscar créditos de Repsol pendientes y enviarlos

        LocalDate hoy = LocalDate.now();
        if (hoy.getDayOfWeek() == DayOfWeek.THURSDAY) {
            log.info("Hoy es jueves, procesando envíos especiales");
            // Lógica adicional según las reglas de negocio
        }
    }

    /**
     * Método manual para reprogramar un crédito
     */
    @Transactional
    public void reprogramarCredito(Long creditoId, LocalDate nuevaFecha) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));

        credito.setProgramadoEnvio(true);
        credito.setFechaProgramadaEnvio(nuevaFecha);
        creditoRepository.save(credito);

        log.info("Crédito {} reprogramado para {}", creditoId, nuevaFecha);
    }
}
