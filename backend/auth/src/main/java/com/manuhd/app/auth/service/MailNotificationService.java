package com.manuhd.app.auth.service;

import com.manuhd.app.auth.model.Incidencia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.developer.email}")
    private String developerEmail;

    @Value("${spring.mail.username:notificaciones@atg.es}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public MailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notificarNuevaIncidencia(Incidencia i) {
        if (!emailEnabled) {
            log.info("Email deshabilitado (app.email.enabled=false), omitiendo notificación de incidencia #{}", i.getId());
            return;
        }
        log.info("Enviando notificación de nueva incidencia #{} a {}", i.getId(), developerEmail);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(developerEmail);
            msg.setSubject("[ATG] Nueva incidencia #" + i.getId() + " - " + i.getTipo() + ": " + i.getTitulo());
            msg.setText("""
                    Se ha creado una nueva incidencia en la aplicación ATG.

                    ID: #%d
                    Tipo: %s
                    Prioridad: %s
                    Título: %s
                    Autor: %s

                    Descripción:
                    %s

                    ---
                    Accede a la aplicación para ver los detalles y gestionar la incidencia.
                    """.formatted(i.getId(), i.getTipo(), i.getPrioridad(),
                    i.getTitulo(), i.getAutor(), i.getDescripcion()));
            mailSender.send(msg);
            log.info("Notificación de incidencia #{} enviada a {}", i.getId(), developerEmail);
        } catch (Exception e) {
            log.error("Error al enviar notificación de incidencia #{}: {}", i.getId(), e.getMessage());
        }
    }
}
