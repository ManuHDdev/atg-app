package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.client.PetrolerasClient;
import com.manuhd.app.contratos.client.SociosClient;
import com.manuhd.app.contratos.exception.BusinessValidationException;
import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.model.SolicitudContrato;
import com.manuhd.app.contratos.model.TipoSolicitudContrato;
import com.manuhd.app.contratos.repository.SolicitudContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de las reglas de adjunto y de "no marcar como enviado lo que no se ha enviado".
 * Sin contexto de Spring, sin base de datos y sin SMTP real: JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudContratoService - envíos con PDF adjunto")
class SolicitudContratoServiceTest {

    private static final Long SOLICITUD_ID = 1L;
    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final String NUMERO_SOLICITUD = "SOL-2026-00001";

    @Mock
    private SolicitudContratoRepository solicitudRepository;

    @Mock
    private PdfService pdfService;

    @Mock
    private PetrolerasClient petrolerasClient;

    @Mock
    private ContratoSocioService contratoSocioService;

    @Mock
    private EmailService emailService;

    @Mock
    private SociosClient sociosClient;

    @InjectMocks
    private SolicitudContratoService service;

    private SociosClient.SocioDTO socio;
    private PetrolerasClient.PetroleraDTO petrolera;

    @BeforeEach
    void setUp() {
        socio = new SociosClient.SocioDTO();
        socio.setId(SOCIO_ID);
        socio.setNombre("Transportes Ejemplo SL");
        socio.setNif("B12345678");
        socio.setNumeroSocio("S-001");
        socio.setEmail("socio@example.com");

        petrolera = new PetrolerasClient.PetroleraDTO();
        petrolera.setId(PETROLERA_ID);
        petrolera.setNombre("Repsol");
        petrolera.setEmail("petrolera@example.com");
    }

    private SolicitudContrato solicitud(EstadoSolicitud estado) {
        SolicitudContrato solicitud = new SolicitudContrato();
        solicitud.setId(SOLICITUD_ID);
        solicitud.setNumeroSolicitud(NUMERO_SOLICITUD);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setTipoSolicitud(TipoSolicitudContrato.NUEVO);
        solicitud.setEstado(estado);
        return solicitud;
    }

    private Path crearPdf(Path dir, String nombre) throws Exception {
        Path pdf = dir.resolve(nombre);
        Files.write(pdf, "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8));
        return pdf;
    }

    // ---------- enviarAPetrolera ----------

    @Test
    @DisplayName("enviarAPetrolera - Sin PDF firmado no cambia de estado y avisa en español")
    void enviarAPetrolera_SinPdfFirmado_NoCambiaEstado() {
        SolicitudContrato solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        solicitud.setRutaPdfFirmado(null);
        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("No se puede enviar a la petrolera: falta el PDF firmado de la solicitud "
                        + NUMERO_SOLICITUD);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
        assertThat(solicitud.getFechaEnvioPetrolera()).isNull();
        verify(solicitudRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoHTMLConAdjunto(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("enviarAPetrolera - Con ruta a un fichero inexistente no cambia de estado")
    void enviarAPetrolera_ConPdfInexistente_NoCambiaEstado(@TempDir Path tempDir) {
        SolicitudContrato solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        solicitud.setRutaPdfFirmado(tempDir.resolve("no-existe.pdf").toString());
        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("falta el PDF firmado");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("enviarAPetrolera - Adjunta el PDF final con nombre legible")
    void enviarAPetrolera_AdjuntaPdfFinal(@TempDir Path tempDir) throws Exception {
        SolicitudContrato solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        Path firmado = crearPdf(tempDir, "firmado.pdf");
        Path finalPdf = crearPdf(tempDir, "final.pdf");
        solicitud.setRutaPdfFirmado(firmado.toString());

        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(SolicitudContrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.copiarPdfFinal(firmado.toString(), NUMERO_SOLICITUD)).thenReturn(finalPdf.toString());
        when(petrolerasClient.obtenerPetrolera(PETROLERA_ID)).thenReturn(petrolera);
        when(sociosClient.obtenerSocio(SOCIO_ID)).thenReturn(socio);
        when(petrolerasClient.obtenerPlantillaCorreo(PETROLERA_ID, "CONTRATO_PETROLERA")).thenReturn(null);

        service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
        assertThat(solicitud.getFechaEnvioPetrolera()).isNotNull();
        verify(emailService).enviarCorreoHTMLConAdjunto(
                eq("petrolera@example.com"),
                anyString(),
                anyString(),
                eq(finalPdf),
                eq("Contrato-firmado-" + NUMERO_SOLICITUD + ".pdf"));
        verify(emailService, never()).enviarCorreoHTML(anyString(), anyString(), anyString());
    }

    // ---------- enviarASocio ----------

    @Test
    @DisplayName("enviarASocio - Sin PDF editable no aplana, no cambia de estado y avisa en español")
    void enviarASocio_SinPdf_NoCambiaEstado() throws Exception {
        SolicitudContrato solicitud = solicitud(EstadoSolicitud.BORRADOR);
        solicitud.setRutaPdfEditable(null);
        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("No se puede enviar al socio: falta el PDF de la solicitud " + NUMERO_SOLICITUD);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(solicitud.getFechaEnvioSocio()).isNull();
        verify(pdfService, never()).aplanarPdfParaSolicitud(anyString(), anyString());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("enviarASocio - Adjunta el PDF aplanado con nombre legible")
    void enviarASocio_AdjuntaPdfAplanado(@TempDir Path tempDir) throws Exception {
        SolicitudContrato solicitud = solicitud(EstadoSolicitud.BORRADOR);
        Path editable = crearPdf(tempDir, "editable.pdf");
        Path enviado = crearPdf(tempDir, "enviado.pdf");
        solicitud.setRutaPdfEditable(editable.toString());

        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(SolicitudContrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.aplanarPdfParaSolicitud(editable.toString(), NUMERO_SOLICITUD))
                .thenReturn(enviado.toString());
        when(sociosClient.obtenerSocio(SOCIO_ID)).thenReturn(socio);
        when(petrolerasClient.obtenerPetrolera(PETROLERA_ID)).thenReturn(petrolera);
        when(petrolerasClient.obtenerPlantillaCorreo(PETROLERA_ID, "NOTIF_SOCIO_CONTRATO_ENVIADO")).thenReturn(null);

        service.enviarASocio(SOLICITUD_ID);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        verify(emailService).enviarCorreoHTMLConAdjunto(
                eq("socio@example.com"),
                anyString(),
                anyString(),
                eq(enviado),
                eq("Contrato-" + NUMERO_SOLICITUD + ".pdf"));
        verify(emailService, never()).enviarCorreoHTML(anyString(), anyString(), anyString());
    }
}
