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
        procesarCreditosProgramados(LocalDate.now());
    }

    /**
     * Sobrecarga con la fecha inyectada para poder testear el job sin depender del día real.
     */
    @Transactional
    void procesarCreditosProgramados(LocalDate hoy) {
        log.info("Iniciando procesamiento de créditos programados");

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
        procesarCreditosPorDiaSemana(LocalDate.now());
    }

    /**
     * Sobrecarga con la fecha inyectada para poder testear el job sin depender del día real.
     */
    @Transactional
    void procesarCreditosPorDiaSemana(LocalDate hoy) {
        String diaHoy = mapearDiaSemana(hoy.getDayOfWeek());
        log.info("Procesando envíos automáticos por día de semana: {}", diaHoy);

        // IMPORTANTE - NO SIMPLIFICAR:
        // Solo entran aquí los créditos SIN fecha de envío programada propia.
        // Un crédito que el gestor programó para una fecha concreta es responsabilidad
        // exclusiva del job diario procesarCreditosProgramados. Si este job por día de
        // semana los incluyera, un crédito programado para el día 20 saldría el día 15
        // simplemente porque su petrolera envía los lunes, contradiciendo la intención
        // explícita del gestor.
        List<Credito> creditosPendientes = creditoRepository
                .findByEstadoSinProgramacionPropia(EstadoCredito.PENDIENTE)
                .stream()
                .filter(this::sinProgramacionPropia)
                .collect(Collectors.toList());
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

    /**
     * Un crédito tiene programación propia cuando el gestor marcó el envío programado
     * y fijó una fecha concreta. Segunda barrera defensiva sobre la consulta del
     * repositorio: la regla de negocio queda escrita también aquí para que no se pierda
     * si alguien toca la query.
     */
    private boolean sinProgramacionPropia(Credito credito) {
        return !(Boolean.TRUE.equals(credito.getProgramadoEnvio())
                && credito.getFechaProgramadaEnvio() != null);
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
