package com.manuhd.app.auth.service;

import com.manuhd.app.auth.model.Incidencia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
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
        if (!emailEnabled) return;
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
        } catch (Exception e) {
            // No interrumpir el flujo si el email falla
        }
    }
}
