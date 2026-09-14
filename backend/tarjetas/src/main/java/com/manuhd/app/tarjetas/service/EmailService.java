package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.dto.EnvioCorreoResult;
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

    public EnvioCorreoResult enviarCorreoSimple(String destinatario, String asunto, String cuerpo) {
        if (!emailEnabled) {
            log.info("Envío de correo SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo: {}", cuerpo);
            return new EnvioCorreoResult(true, "SIMPLE", destinatario);
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(emailFrom);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            mailSender.send(mensaje);
            log.info("Correo enviado exitosamente a: {}", destinatario);
            return new EnvioCorreoResult(true, "SIMPLE", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
            return new EnvioCorreoResult(false, "SIMPLE", destinatario, e.getMessage());
        }
    }

    public EnvioCorreoResult enviarCorreoHTML(String destinatario, String asunto, String cuerpoHTML) {
        if (!emailEnabled) {
            log.info("Envío de correo HTML SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Cuerpo HTML: {}", cuerpoHTML);
            return new EnvioCorreoResult(true, "HTML", destinatario);
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
            return new EnvioCorreoResult(true, "HTML", destinatario);
        } catch (MessagingException e) {
            log.error("Error al enviar correo HTML a {}: {}", destinatario, e.getMessage());
            return new EnvioCorreoResult(false, "HTML", destinatario, e.getMessage());
        }
    }

    /**
     * Correo HTML con un PDF adjunto. Devuelve el resultado en lugar de lanzar, igual que el
     * resto de envíos, para que quien lo llame pueda registrarlo sin abortar la transición.
     */
    public EnvioCorreoResult enviarCorreoHTMLConAdjunto(String destinatario, String asunto, String cuerpoHTML,
                                                        Path adjunto, String nombreAdjunto) {
        if (!emailEnabled) {
            log.info("Envío de correo HTML con adjunto SIMULADO (email.enabled=false)");
            log.info("Destinatario: {}", destinatario);
            log.info("Asunto: {}", asunto);
            log.info("Adjunto: {} (ruta: {})", nombreAdjunto, adjunto);
            return new EnvioCorreoResult(true, "HTML_ADJUNTO", destinatario);
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
            return new EnvioCorreoResult(true, "HTML_ADJUNTO", destinatario);
        } catch (MessagingException e) {
            log.error("Error al enviar correo HTML con adjunto a {}: {}", destinatario, e.getMessage());
            return new EnvioCorreoResult(false, "HTML_ADJUNTO", destinatario, e.getMessage());
        }
    }

    /**
     * Variante con adjunto de {@link #enviarCorreoConPlantilla}: procesa asunto y cuerpo con
     * las variables y etiqueta el resultado con el tipo de plantilla usado.
     */
    public EnvioCorreoResult enviarCorreoConPlantillaYAdjunto(String destinatario, String asunto, String plantilla,
                                                              Map<String, String> variables, String tipoPlantilla,
                                                              Path adjunto, String nombreAdjunto) {
        EnvioCorreoResult resultado = enviarCorreoHTMLConAdjunto(
                destinatario,
                procesarPlantilla(asunto, variables),
                procesarPlantilla(plantilla, variables),
                adjunto,
                nombreAdjunto);
        resultado.setTipoPlantilla(tipoPlantilla);
        return resultado;
    }

    public String procesarPlantilla(String plantilla, Map<String, String> variables) {
        String resultado = plantilla;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String valor = entry.getValue() != null ? entry.getValue() : "";
            resultado = resultado.replace("{" + entry.getKey() + "}", valor);
        }
        return resultado;
    }

    public EnvioCorreoResult enviarCorreoConPlantilla(String destinatario, String asunto, String plantilla, Map<String, String> variables, String tipoPlantilla) {
        String asuntoFinal = procesarPlantilla(asunto, variables);
        String cuerpoFinal = procesarPlantilla(plantilla, variables);
        EnvioCorreoResult resultado = enviarCorreoHTML(destinatario, asuntoFinal, cuerpoFinal);
        // Actualizar el tipo de plantilla en el resultado
        resultado.setTipoPlantilla(tipoPlantilla);
        return resultado;
    }
}
