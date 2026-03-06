package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest extends TestBase {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@contratos.com");
    }

    @Test
    @DisplayName("enviarCorreoSimple - Debe enviar email cuando está habilitado")
    void enviarCorreoSimple_DebeEnviarEmailCuandoEstaHabilitado() {
        // Given
        String destinatario = "destino@test.com";
        String asunto = "Asunto de prueba";
        String mensaje = "Mensaje de prueba";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.enviarCorreoSimple(destinatario, asunto, mensaje);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage emailCapturado = captor.getValue();
        assertThat(emailCapturado.getTo()).containsExactly(destinatario);
        assertThat(emailCapturado.getFrom()).isEqualTo("test@contratos.com");
        assertThat(emailCapturado.getSubject()).isEqualTo(asunto);
        assertThat(emailCapturado.getText()).isEqualTo(mensaje);
    }

    @Test
    @DisplayName("enviarCorreoSimple - No debe enviar cuando está deshabilitado")
    void enviarCorreoSimple_NoDebeEnviarCuandoEstaDeshabilitado() {
        // Given
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        String destinatario = "destino@test.com";
        String asunto = "Asunto";
        String mensaje = "Mensaje";

        // When
        emailService.enviarCorreoSimple(destinatario, asunto, mensaje);

        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoHTML - Debe enviar email HTML cuando está habilitado")
    void enviarCorreoHTML_DebeEnviarEmailHTMLCuandoEstaHabilitado() throws MessagingException {
        // Given
        String destinatario = "destino@test.com";
        String asunto = "Asunto HTML";
        String contenidoHtml = "<html><body><h1>Test</h1></body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        emailService.enviarCorreoHTML(destinatario, asunto, contenidoHtml);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoHTML - No debe enviar cuando está deshabilitado")
    void enviarCorreoHTML_NoDebeEnviarCuandoEstaDeshabilitado() {
        // Given
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        String destinatario = "destino@test.com";
        String asunto = "Asunto";
        String contenidoHtml = "<html><body>Test</body></html>";

        // When
        emailService.enviarCorreoHTML(destinatario, asunto, contenidoHtml);

        // Then
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoSimple - Debe manejar excepciones al enviar")
    void enviarCorreoSimple_DebeManejarExcepcionesAlEnviar() {
        // Given
        String destinatario = "destino@test.com";
        String asunto = "Asunto";
        String mensaje = "Mensaje";

        doThrow(new RuntimeException("Error al enviar email"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.enviarCorreoSimple(destinatario, asunto, mensaje))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar email");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoHTML - Debe manejar excepciones MessagingException")
    void enviarCorreoHTML_DebeManejarExcepcionesMessagingException() throws MessagingException {
        // Given
        String destinatario = "destino@test.com";
        String asunto = "Asunto";
        String contenidoHtml = "<html><body>Test</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Error al preparar mensaje"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.enviarCorreoHTML(destinatario, asunto, contenidoHtml))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al preparar mensaje");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarCorreoSimple - Debe funcionar con múltiples destinatarios")
    void enviarCorreoSimple_DebeFuncionarConMultiplesDestinatarios() {
        // Given
        String destinatarios = "destino1@test.com,destino2@test.com";
        String asunto = "Asunto múltiple";
        String mensaje = "Mensaje para todos";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.enviarCorreoSimple(destinatarios, asunto, mensaje);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage emailCapturado = captor.getValue();
        assertThat(emailCapturado.getTo()).containsExactly(destinatarios);
    }
}
