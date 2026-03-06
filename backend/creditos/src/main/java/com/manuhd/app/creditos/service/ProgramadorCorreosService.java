package com.manuhd.app.creditos.service;

import com.manuhd.app.creditos.model.Credito;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.repository.CreditoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProgramadorCorreosService {

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private CreditoService creditoService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    /**
     * Tarea programada que se ejecuta todos los días a las 9:00 AM
     * Verifica créditos con fecha de envío programada para hoy
     */
    @Scheduled(cron = "0 0 9 * * *")
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
     * Tarea programada que se ejecuta todos los días a las 8:00 AM
     * Envía créditos pendientes a las petroleras que tienen configurado ese día de la semana
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void procesarCreditosPorDiaSemana() {
        String diaHoy = mapearDiaSemana(LocalDate.now().getDayOfWeek());
        log.info("Procesando envíos automáticos por día de semana: {}", diaHoy);

        List<Credito> creditosPendientes = creditoRepository.findByEstado(EstadoCredito.PENDIENTE);
        if (creditosPendientes.isEmpty()) {
            log.info("No hay créditos pendientes para enviar");
            return;
        }

        Map<Long, List<Credito>> porPetrolera = creditosPendientes.stream()
                .collect(Collectors.groupingBy(Credito::getPetroleraId));

        for (Map.Entry<Long, List<Credito>> entry : porPetrolera.entrySet()) {
            Long petroleraId = entry.getKey();
            List<Credito> creditos = entry.getValue();

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> petrolera = restTemplate.getForObject(
                        petrolerasBaseUrl + "/api/petroleras/" + petroleraId, Map.class);

                if (petrolera == null) continue;

                String diasEnvio = (String) petrolera.get("diasEnvioCreditos");
                if (diasEnvio == null || diasEnvio.isBlank()) continue;

                List<String> diasLista = Arrays.asList(diasEnvio.split(","));
                if (!diasLista.contains(diaHoy)) continue;

                log.info("Enviando {} créditos pendientes de petrolera {} (día: {})",
                        creditos.size(), petroleraId, diaHoy);

                for (Credito credito : creditos) {
                    try {
                        creditoService.enviarAPetrolera(credito.getId());
                        log.info("Crédito {} enviado automáticamente a petrolera {}", credito.getId(), petroleraId);
                    } catch (Exception e) {
                        log.error("Error al enviar crédito {} automáticamente: {}", credito.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error al procesar envíos de petrolera {}: {}", petroleraId, e.getMessage());
            }
        }

        log.info("Finalizado procesamiento de envíos automáticos por día de semana");
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

    private String mapearDiaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}
