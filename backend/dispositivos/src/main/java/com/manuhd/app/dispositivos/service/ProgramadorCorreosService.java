package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.repository.SolicitudDispositivoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ProgramadorCorreosService {

    @Autowired
    private SolicitudDispositivoRepository solicitudRepository;

    @Autowired
    private SolicitudDispositivoService solicitudService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void procesarSolicitudesProgramadas() {
        log.info("Iniciando procesamiento de solicitudes de dispositivo programadas");

        LocalDate hoy = LocalDate.now();
        List<SolicitudDispositivo> programadas = solicitudRepository
                .findByProgramadoEnvioTrueAndFechaProgramadaEnvio(hoy);

        log.info("Encontradas {} solicitudes programadas para hoy", programadas.size());

        for (SolicitudDispositivo solicitud : programadas) {
            try {
                log.info("Enviando solicitud programada ID: {}", solicitud.getId());
                solicitudService.enviarAPetrolera(solicitud.getId());
                log.info("Solicitud {} enviada exitosamente", solicitud.getId());
            } catch (Exception e) {
                log.error("Error al enviar solicitud programada {}: {}", solicitud.getId(), e.getMessage());
            }
        }

        log.info("Finalizado procesamiento de solicitudes programadas");
    }
}
