package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.model.EstadoSolicitud;
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
            // Con el circuito del documento firmado, una solicitud solo se presenta a la
            // petrolera cuando el socio ha devuelto el impreso firmado y la oficina lo ha
            // aceptado. El envio programado no puede saltarse ese paso: si todavia no esta
            // firmada se deja para otro dia en lugar de fallar.
            if (solicitud.getEstado() != EstadoSolicitud.FIRMADO_SOCIO) {
                log.info("Solicitud programada {} omitida: esta en estado {} y aun no tiene la firma del socio aceptada",
                        solicitud.getId(), solicitud.getEstado());
                continue;
            }

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
