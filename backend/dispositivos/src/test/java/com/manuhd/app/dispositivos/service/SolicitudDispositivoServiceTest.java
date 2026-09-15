package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.client.PetrolerasClient;
import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.PetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.model.Dispositivo;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import com.manuhd.app.dispositivos.repository.DispositivoRepository;
import com.manuhd.app.dispositivos.repository.SolicitudDispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de las restricciones de petrolera al crear solicitudes de dispositivo.
 *
 * El procedimiento de ATG restringe con qué petroleras trabaja cada módulo: dispositivos
 * solo opera con Cepsa/Moeve y Repsol, y la "solicitud de crédito" de un dispositivo es
 * exclusiva de Moeve (Cepsa). Esas restricciones son configurables por petrolera y un flag
 * sin valor (null) significa "sin restricción configurada" => permitido.
 *
 * Sin contexto de Spring ni base de datos: solo JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudDispositivoServiceTest {

    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final Long DISPOSITIVO_ID = 30L;
    private static final String PETROLERAS_URL = "http://petroleras:8082";
    private static final String URL_PETROLERA = PETROLERAS_URL + "/api/petroleras/" + PETROLERA_ID;

    @Mock
    private SolicitudDispositivoRepository solicitudRepository;

    @Mock
    private DispositivoRepository dispositivoRepository;

    @Mock
    private DispositivoService dispositivoService;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PetrolerasClient petrolerasClient;

    @InjectMocks
    private SolicitudDispositivoService service;

    /**
     * PdfService real (no mock) sobre un directorio temporal: el circuito del documento
     * firmado comprueba que los ficheros existen de verdad antes de dar una etapa por hecha,
     * asi que los tests trabajan con ficheros reales.
     */
    private PdfService pdfService;

    @TempDir
    Path directorioStorage;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(service, "petrolerasBaseUrl", PETROLERAS_URL);
        ReflectionTestUtils.setField(service, "sociosBaseUrl", "http://socios:8081");

        pdfService = new PdfService();
        ReflectionTestUtils.setField(pdfService, "dispositivosPath", directorioStorage.toString());
        ReflectionTestUtils.setField(service, "pdfService", pdfService);

        when(petrolerasClient.obtenerPlantillaDocumento(anyLong(), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
        when(solicitudRepository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);

        // La solicitud guardada se devuelve tal cual, con un id asignado
        when(solicitudRepository.save(any(SolicitudDispositivo.class))).thenAnswer(invocation -> {
            SolicitudDispositivo guardada = invocation.getArgument(0);
            guardada.setId(1L);
            return guardada;
        });
    }

    // --- Datos de apoyo ---------------------------------------------------------------

    private PetroleraDTO petrolera(Boolean operaDispositivos, Boolean permiteCreditoDispositivo) {
        PetroleraDTO dto = new PetroleraDTO();
        dto.setId(PETROLERA_ID);
        dto.setNombre("Galp");
        dto.setActiva(true);
        dto.setOperaDispositivos(operaDispositivos);
        dto.setPermiteCreditoDispositivo(permiteCreditoDispositivo);
        return dto;
    }

    private void darDeAltaPetrolera(PetroleraDTO petrolera) {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class))).thenReturn(petrolera);
    }

    private CrearSolicitudDTO solicitudAlta() {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setTipoSolicitud(TipoSolicitud.ALTA_DISPOSITIVO);
        dto.setMatricula("1234abc");
        // Programada: evita el envío inmediato de correo a la petrolera
        dto.setProgramadoEnvio(true);
        return dto;
    }

    private CrearSolicitudDTO solicitudCredito() {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setTipoSolicitud(TipoSolicitud.SOLICITUD_CREDITO);
        dto.setDispositivoId(DISPOSITIVO_ID);
        dto.setMonto(new BigDecimal("1500.00"));
        dto.setProgramadoEnvio(true);
        return dto;
    }

    private void dispositivoActivo() {
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setId(DISPOSITIVO_ID);
        dispositivo.setMatricula("1234ABC");
        dispositivo.setActivo(true);
        when(dispositivoRepository.findById(DISPOSITIVO_ID)).thenReturn(java.util.Optional.of(dispositivo));
    }

    // --- Tests ------------------------------------------------------------------------

    @Test
    void rechazaLaSolicitudSiLaPetroleraNoOperaConDispositivos() {
        darDeAltaPetrolera(petrolera(false, null));
        when(dispositivoService.validarMatriculaDisponible(any())).thenReturn(true);

        assertThatThrownBy(() -> service.crear(solicitudAlta()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Galp")
                .hasMessageContaining("no opera con dispositivos");

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }

    @Test
    void rechazaLaSolicitudDeCreditoSiLaPetroleraNoLaAdmite() {
        darDeAltaPetrolera(petrolera(true, false));
        dispositivoActivo();

        assertThatThrownBy(() -> service.crear(solicitudCredito()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Galp")
                .hasMessageContaining("no admite solicitudes de credito para dispositivos");

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }

    @Test
    void permiteLaSolicitudDeCreditoCuandoLosFlagsEstanSinConfigurar() {
        // null = sin restricción configurada => se permite (comportamiento previo intacto)
        darDeAltaPetrolera(petrolera(null, null));
        dispositivoActivo();

        SolicitudDispositivoDTO creada = service.crear(solicitudCredito());

        assertThat(creada.getTipoSolicitud()).isEqualTo(TipoSolicitud.SOLICITUD_CREDITO);
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(creada.getMatricula()).isEqualTo("1234ABC");
        verify(solicitudRepository).save(any(SolicitudDispositivo.class));
    }

    @Test
    void permiteUnAltaNormalCuandoLaPetroleraOperaConDispositivos() {
        darDeAltaPetrolera(petrolera(true, false));
        when(dispositivoService.validarMatriculaDisponible("1234abc")).thenReturn(true);

        SolicitudDispositivoDTO creada = service.crear(solicitudAlta());

        assertThat(creada.getTipoSolicitud()).isEqualTo(TipoSolicitud.ALTA_DISPOSITIVO);
        assertThat(creada.getMatricula()).isEqualTo("1234ABC");
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        verify(solicitudRepository).save(any(SolicitudDispositivo.class));
    }

    @Test
    void rechazaLaSolicitudSiNoSePuedeConsultarLaPetrolera() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.crear(solicitudAlta()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se ha podido verificar la petrolera");

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }

    @Test
    void rechazaLaSolicitudSiLaPetroleraNoExiste() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class))).thenReturn(null);

        assertThatThrownBy(() -> service.crear(solicitudAlta()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Petrolera no encontrada");

        verify(dispositivoRepository, never()).findById(anyLong());
    }

    // --- Importe concedido por la petrolera ---------------------------------------------

    private SolicitudDispositivo solicitudEnviada(TipoSolicitud tipo, BigDecimal monto) {
        SolicitudDispositivo solicitud = new SolicitudDispositivo();
        solicitud.setId(1L);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setTipoSolicitud(tipo);
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setMonto(monto);
        solicitud.setMatricula("1234ABC");
        when(solicitudRepository.findById(1L)).thenReturn(java.util.Optional.of(solicitud));

        // La notificacion al socio consulta los microservicios de socios y petroleras
        java.util.Map<String, Object> socio = new java.util.HashMap<>();
        socio.put("nombre", "Transportes Perez");
        socio.put("email", "socio@example.com");
        when(restTemplate.getForObject(eq("http://socios:8081/api/socios/" + SOCIO_ID), eq(java.util.Map.class)))
                .thenReturn(socio);
        java.util.Map<String, Object> petrolera = new java.util.HashMap<>();
        petrolera.put("nombre", "Cepsa (Moeve)");
        petrolera.put("email", "petrolera@example.com");
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(java.util.Map.class))).thenReturn(petrolera);
        return solicitud;
    }

    @Test
    void registraElImporteConcedidoAlAprobarUnaSolicitudDeCredito() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.SOLICITUD_CREDITO, new BigDecimal("12000.00"));

        service.responderPetrolera(1L, true, "OK", new BigDecimal("12000.00"));

        assertThat(solicitud.getMontoConcedido()).isEqualByComparingTo("12000.00");
    }

    @Test
    void persisteUnImporteConcedidoDistintoDelSolicitado() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        service.responderPetrolera(1L, true, "Ampliado", new BigDecimal("4000.00"));

        assertThat(solicitud.getMonto()).isEqualByComparingTo("2000.00");
        assertThat(solicitud.getMontoConcedido()).isEqualByComparingTo("4000.00");
    }

    @Test
    void rechazaLaAprobacionDeCreditoSinImporteConcedido() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("importe concedido");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
    }

    @Test
    void rechazaLaAprobacionDeCreditoConImporteConcedidoCeroONegativo() {
        solicitudEnviada(TipoSolicitud.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor que 0");

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", new BigDecimal("-1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor que 0");
    }

    @Test
    void alDenegarNoSeGuardaImporteConcedido() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        service.responderPetrolera(1L, false, "Denegado", new BigDecimal("2000.00"));

        assertThat(solicitud.getMontoConcedido()).isNull();
    }

    @Test
    void unTipoSinImporteSeApruebaSinImporteConcedido() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.BAJA_DISPOSITIVO, null);
        solicitud.setDispositivoId(DISPOSITIVO_ID);
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setId(DISPOSITIVO_ID);
        dispositivo.setMatricula("1234ABC");
        dispositivo.setActivo(true);
        when(dispositivoRepository.findById(DISPOSITIVO_ID)).thenReturn(java.util.Optional.of(dispositivo));

        // Aunque llegue un importe, un tipo que no es credito no lo guarda
        service.responderPetrolera(1L, true, "OK", new BigDecimal("500.00"));

        assertThat(solicitud.getMontoConcedido()).isNull();
    }

    // --- Circuito del documento firmado -------------------------------------------------

    /** PDF real minimo: el aplanado del impreso lo abre PDFBox de verdad. */
    private byte[] pdfDeUnaPagina() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private MockMultipartFile ficheroPdf(String nombre) throws IOException {
        return new MockMultipartFile("file", nombre, "application/pdf", pdfDeUnaPagina());
    }

    /** Datos de socio y petrolera que consultan las transiciones del circuito. */
    private void darDeAltaDatosRelacionados() {
        java.util.Map<String, Object> socio = new java.util.HashMap<>();
        socio.put("nombre", "Transportes Perez");
        socio.put("email", "socio@example.com");
        when(restTemplate.getForObject(eq("http://socios:8081/api/socios/" + SOCIO_ID), eq(java.util.Map.class)))
                .thenReturn(socio);

        java.util.Map<String, Object> petrolera = new java.util.HashMap<>();
        petrolera.put("nombre", "Cepsa (Moeve)");
        petrolera.put("email", "petrolera@example.com");
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(java.util.Map.class))).thenReturn(petrolera);
    }

    /** Plantilla de correo configurada en petroleras para el tipo indicado. */
    private void darDeAltaPlantillaCorreo(String tipoPlantilla) {
        java.util.Map<String, Object> plantilla = new java.util.HashMap<>();
        plantilla.put("asunto", "Su solicitud de dispositivo");
        plantilla.put("cuerpo", "<p>Hola {{socio_nombre}}</p>");
        String url = String.format("%s/api/plantillas-correo/petrolera/%d?tipo=%s",
                PETROLERAS_URL, PETROLERA_ID, tipoPlantilla);
        when(restTemplate.getForObject(eq(url), eq(java.util.Map.class))).thenReturn(plantilla);
    }

    /** Solicitud ya persistida en el estado indicado, con numero de solicitud asignado. */
    private SolicitudDispositivo solicitudEnCircuito(EstadoSolicitud estado) {
        SolicitudDispositivo solicitud = new SolicitudDispositivo();
        solicitud.setId(1L);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setTipoSolicitud(TipoSolicitud.ALTA_DISPOSITIVO);
        solicitud.setEstado(estado);
        solicitud.setMatricula("1234ABC");
        solicitud.setNumeroSolicitud("DIS-" + Year.now().getValue() + "-00001");
        when(solicitudRepository.findById(1L)).thenReturn(java.util.Optional.of(solicitud));
        darDeAltaDatosRelacionados();
        return solicitud;
    }

    /** Deja en disco el impreso editable de la solicitud, como si se hubiera descargado. */
    private void conImpresoEditable(SolicitudDispositivo solicitud) throws IOException {
        solicitud.setRutaPdfEditable(
                pdfService.copiarPlantillaParaSolicitud(pdfDeUnaPagina(), solicitud.getNumeroSolicitud()));
        solicitud.setNombrePdfEditable(PdfService.EDITABLE);
    }

    /** Deja en disco el impreso firmado devuelto por el socio. */
    private void conImpresoFirmado(SolicitudDispositivo solicitud) throws IOException {
        solicitud.setRutaPdfFirmado(
                pdfService.guardarPdfFirmado(ficheroPdf("firmado.pdf"), solicitud.getNumeroSolicitud()));
        solicitud.setNombrePdfFirmado("firmado.pdf");
    }

    @Test
    void laSolicitudNaceEnBorradorConSuNumeroYSuImpreso() {
        darDeAltaPetrolera(petrolera(true, false));
        when(dispositivoService.validarMatriculaDisponible("1234abc")).thenReturn(true);

        SolicitudDispositivoDTO creada = service.crear(solicitudAlta());

        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(creada.getNumeroSolicitud()).isEqualTo("DIS-" + Year.now().getValue() + "-00001");
        assertThat(creada.getRutaPdfEditable()).endsWith(PdfService.EDITABLE);
        assertThat(Path.of(creada.getRutaPdfEditable())).exists();
    }

    @Test
    void noAvisaALaPetroleraAlCrearLaSolicitud() {
        // La solicitud nace en BORRADOR: todavia no se ha presentado nada a la petrolera.
        darDeAltaPetrolera(petrolera(true, false));
        when(dispositivoService.validarMatriculaDisponible("1234abc")).thenReturn(true);
        darDeAltaDatosRelacionados();

        service.crear(solicitudAlta());

        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(
                any(), any(), any(), anyMap(), any(Path.class), any());
        verify(emailService, never()).enviarCorreoConPlantilla(eq("petrolera@example.com"), any(), any(), anyMap());
        verify(emailService, never()).enviarCorreoHTML(eq("petrolera@example.com"), any(), any());
    }

    @Test
    void enviarASocioExigeQueLaSolicitudEsteEnBorrador() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.ENVIADO_SOCIO);

        assertThatThrownBy(() -> service.enviarASocio(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("borrador");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void enviarASocioSinImpresoNiCambiaElEstadoNiEnviaCorreo() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.BORRADOR);

        assertThatThrownBy(() -> service.enviarASocio(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("falta el impreso");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(solicitud.getFechaEnvioSocio()).isNull();
        verifyNoInteractions(emailService);
    }

    @Test
    void enviarASocioAplanaElImpresoYLoAdjuntaAlCorreoDelSocio() throws IOException {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        darDeAltaPlantillaCorreo("DOCUMENTO_SOCIO_DISPOSITIVO");

        service.enviarASocio(1L);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(solicitud.getFechaEnvioSocio()).isNotNull();
        assertThat(solicitud.getRutaPdfEnviado()).endsWith(PdfService.ENVIADO);

        ArgumentCaptor<Path> adjunto = ArgumentCaptor.forClass(Path.class);
        verify(emailService).enviarCorreoConPlantillaYAdjunto(
                eq("socio@example.com"), any(), any(), anyMap(), adjunto.capture(), any());
        assertThat(adjunto.getValue()).hasFileName(PdfService.ENVIADO).exists();
    }

    @Test
    void subirElImpresoFirmadoNoCambiaElEstado() throws IOException {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(1L, ficheroPdf("escaneado.pdf"));

        // A proposito: el escaneado se puede sustituir tantas veces como haga falta y es
        // aceptarFirmaSocio() quien cierra la etapa.
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(solicitud.getRutaPdfFirmado()).endsWith(PdfService.FIRMADO);
        assertThat(solicitud.getFechaRecepcionFirmado()).isNotNull();
    }

    @Test
    void subirElImpresoFirmadoExigeQueSeHayaEnviadoAlSocio() throws IOException {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.BORRADOR);
        MockMultipartFile fichero = ficheroPdf("escaneado.pdf");

        assertThatThrownBy(() -> service.subirPdfFirmado(1L, fichero))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("enviada al socio");

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
    }

    @Test
    void aceptarLaFirmaExigeElImpresoFirmado() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.ENVIADO_SOCIO);

        assertThatThrownBy(() -> service.aceptarFirmaSocio(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("impreso firmado");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void aceptarLaFirmaExigeQueLaSolicitudEsteEnviadaAlSocio() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.BORRADOR);

        assertThatThrownBy(() -> service.aceptarFirmaSocio(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("enviada al socio");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
    }

    @Test
    void aceptarLaFirmaDejaLaSolicitudListaParaLaPetrolera() throws IOException {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.ENVIADO_SOCIO);
        conImpresoFirmado(solicitud);

        service.aceptarFirmaSocio(1L);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
    }

    @Test
    void enviarALaPetroleraExigeLaFirmaDelSocioAceptada() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.ENVIADO_SOCIO);

        assertThatThrownBy(() -> service.enviarAPetrolera(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("firma del socio aceptada");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        verifyNoInteractions(emailService);
    }

    @Test
    void enviarALaPetroleraSinImpresoFirmadoNoEnviaNada() {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.FIRMADO_SOCIO);

        assertThatThrownBy(() -> service.enviarAPetrolera(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("falta el impreso firmado");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
        assertThat(solicitud.getFechaEnvioPetrolera()).isNull();
        verifyNoInteractions(emailService);
    }

    @Test
    void enviarALaPetroleraAdjuntaElImpresoFirmado() throws IOException {
        SolicitudDispositivo solicitud = solicitudEnCircuito(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);

        service.enviarAPetrolera(1L);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
        assertThat(solicitud.getFechaEnvioPetrolera()).isNotNull();
        assertThat(solicitud.getRutaPdfFinal()).endsWith(PdfService.FINAL);

        ArgumentCaptor<Path> adjunto = ArgumentCaptor.forClass(Path.class);
        verify(emailService).enviarCorreoConPlantillaYAdjunto(
                eq("petrolera@example.com"), any(), any(), anyMap(), adjunto.capture(), any());
        assertThat(adjunto.getValue()).hasFileName(PdfService.FINAL).exists();
    }

    @Test
    void alDenegarSeGuardaElMotivoDelRechazo() {
        SolicitudDispositivo solicitud = solicitudEnviada(TipoSolicitud.BAJA_DISPOSITIVO, null);

        service.responderPetrolera(1L, false, "Matricula no reconocida", null);

        // responderPetrolera encadena la notificacion al socio, que cierra la solicitud;
        // el motivo del rechazo tiene que sobrevivir a ese paso en su propio campo.
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADO);
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("Matricula no reconocida");
    }

    // --- Estado heredado PENDIENTE ------------------------------------------------------

    @Test
    void unaSolicitudEnElEstadoHeredadoPendienteSigueSiendoLegibleYProcesable() throws IOException {
        // Filas creadas antes del circuito del documento firmado: sin numero de solicitud y
        // paradas en PENDIENTE. Deben poder leerse y presentarse a la petrolera como antes.
        SolicitudDispositivo solicitud = new SolicitudDispositivo();
        solicitud.setId(1L);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setTipoSolicitud(TipoSolicitud.ALTA_DISPOSITIVO);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setMatricula("1234ABC");
        solicitud.setNumeroSolicitud(null);
        when(solicitudRepository.findById(1L)).thenReturn(java.util.Optional.of(solicitud));
        darDeAltaDatosRelacionados();

        SolicitudDispositivoDTO leida = service.obtenerPorId(1L);
        assertThat(leida.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(leida.getNumeroSolicitud()).isNull();

        service.enviarAPetrolera(1L);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
        // Sin impreso que adjuntar: se envia el correo antiguo, no el del circuito de firma
        verify(emailService).enviarCorreoConPlantilla(eq("petrolera@example.com"), any(), any(), anyMap());
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(
                any(), any(), any(), anyMap(), any(Path.class), any());
    }
}
