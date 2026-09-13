package com.manuhd.app.contratos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.nio.file.Path;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@atg.com}")
    private String emailFrom;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public void enviarCorreoSimple(String destinatario, String asunto, String cuerpo) {
        if (!emailEnabled) {
            log.info("Envío de correo SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo: {}", cuerpo);
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(emailFrom);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            mailSender.send(mensaje);
            log.info("Correo enviado exitosamente a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo", e);
        }
    }

    public void enviarCorreoHTML(String destinatario, String asunto, String cuerpoHTML) {
        if (!emailEnabled) {
            log.info("Envío de correo HTML SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo HTML: {}", cuerpoHTML);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHTML, true);

            mailSender.send(mensaje);
            log.info("Correo HTML enviado exitosamente a: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Error al enviar correo HTML a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo HTML", e);
        }
    }

    /**
     * Envía un correo HTML con un único fichero adjunto.
     *
     * @param destinatario  dirección de destino
     * @param asunto        asunto del correo
     * @param cuerpoHTML    cuerpo del correo en HTML
     * @param adjunto       ruta del fichero a adjuntar
     * @param nombreAdjunto nombre legible con el que se adjunta el fichero
     */
    public void enviarCorreoHTMLConAdjunto(String destinatario, String asunto, String cuerpoHTML,
                                           Path adjunto, String nombreAdjunto) {
        if (!emailEnabled) {
            log.info("Envío de correo HTML con adjunto SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo HTML: {}", cuerpoHTML);
            log.info("Adjunto: {} (ruta: {})", nombreAdjunto, adjunto);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHTML, true);
            helper.addAttachment(nombreAdjunto, new FileSystemResource(adjunto.toFile()));

            mailSender.send(mensaje);
            log.info("Correo HTML con adjunto '{}' enviado exitosamente a: {}", nombreAdjunto, destinatario);
        } catch (MessagingException e) {
            log.error("Error al enviar correo HTML con adjunto a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo HTML con adjunto", e);
        }
    }

    public String procesarPlantilla(String plantilla, Map<String, String> variables) {
        String resultado = plantilla;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resultado = resultado.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resultado;
    }

    public void enviarCorreoConPlantilla(String destinatario, String asunto, String plantilla, Map<String, String> variables) {
        String cuerpoFinal = procesarPlantilla(plantilla, variables);
        enviarCorreoHTML(destinatario, asunto, cuerpoFinal);
    }
}
