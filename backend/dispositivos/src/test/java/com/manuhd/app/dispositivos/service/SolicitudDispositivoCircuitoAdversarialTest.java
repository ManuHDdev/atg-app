package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.client.PetrolerasClient;
import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.PetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.exception.BusinessValidationException;
import com.manuhd.app.dispositivos.model.Dispositivo;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import com.manuhd.app.dispositivos.repository.DispositivoRepository;
import com.manuhd.app.dispositivos.repository.SolicitudDispositivoRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas adversariales del circuito del documento firmado de dispositivos: cada transicion
 * invocada desde el estado equivocado, repetida, con ficheros que no son impresos, con el
 * PDF borrado del disco entre dos pasos y con los servicios externos caidos.
 *
 * Lo que se comprueba no es el camino feliz (ya cubierto en
 * {@link SolicitudDispositivoServiceTest}) sino que el circuito no avanza de estado, no crea
 * dispositivos de mas y no escribe fuera del directorio de la solicitud cuando se le maltrata.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudDispositivoCircuitoAdversarialTest {

    private static final Long SOLICITUD_ID = 1L;
    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final Long DISPOSITIVO_ID = 30L;
    private static final String SOCIOS_URL = "http://socios:8081";
    private static final String PETROLERAS_URL = "http://petroleras:8082";
    private static final String URL_PETROLERA = PETROLERAS_URL + "/api/petroleras/" + PETROLERA_ID;
    private static final String NUMERO_SOLICITUD = "DIS-" + Year.now().getValue() + "-00001";

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

    /** PdfService real sobre disco: el circuito comprueba ficheros de verdad. */
    private PdfService pdfService;

    /** Raiz de trabajo: dentro vive el almacen, y al lado el "fuera" que nadie debe tocar. */
    @TempDir
    Path raiz;

    private Path almacen;

    @BeforeEach
    void setUp() throws IOException {
        almacen = Files.createDirectories(raiz.resolve("storage"));

        ReflectionTestUtils.setField(service, "petrolerasBaseUrl", PETROLERAS_URL);
        ReflectionTestUtils.setField(service, "sociosBaseUrl", SOCIOS_URL);

        pdfService = new PdfService();
        ReflectionTestUtils.setField(pdfService, "dispositivosPath", almacen.toString());
        ReflectionTestUtils.setField(service, "pdfService", pdfService);

        when(petrolerasClient.obtenerPlantillaDocumento(anyLong(), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
        when(solicitudRepository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);
        when(solicitudRepository.save(any(SolicitudDispositivo.class))).thenAnswer(i -> {
            SolicitudDispositivo guardada = i.getArgument(0);
            guardada.setId(SOLICITUD_ID);
            return guardada;
        });
    }

    // ---------- helpers ----------

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

    private void darDeAltaDatosRelacionados() {
        Map<String, Object> socio = new HashMap<>();
        socio.put("nombre", "Transportes Perez");
        socio.put("email", "socio@example.com");
        when(restTemplate.getForObject(eq(SOCIOS_URL + "/api/socios/" + SOCIO_ID), eq(Map.class)))
                .thenReturn(socio);

        Map<String, Object> petrolera = new HashMap<>();
        petrolera.put("nombre", "Cepsa (Moeve)");
        petrolera.put("email", "petrolera@example.com");
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(Map.class))).thenReturn(petrolera);
    }

    private SolicitudDispositivo solicitud(EstadoSolicitud estado) {
        SolicitudDispositivo solicitud = new SolicitudDispositivo();
        solicitud.setId(SOLICITUD_ID);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setTipoSolicitud(TipoSolicitud.ALTA_DISPOSITIVO);
        solicitud.setEstado(estado);
        solicitud.setMatricula("1234ABC");
        solicitud.setNumeroSolicitud(NUMERO_SOLICITUD);
        when(solicitudRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        darDeAltaDatosRelacionados();
        return solicitud;
    }

    private void conImpresoEditable(SolicitudDispositivo solicitud) throws IOException {
        solicitud.setRutaPdfEditable(
                pdfService.copiarPlantillaParaSolicitud(pdfDeUnaPagina(), NUMERO_SOLICITUD));
        solicitud.setNombrePdfEditable(PdfService.EDITABLE);
    }

    private void conImpresoFirmado(SolicitudDispositivo solicitud) throws IOException {
        solicitud.setRutaPdfFirmado(
                pdfService.guardarPdfFirmado(ficheroPdf("firmado.pdf"), NUMERO_SOLICITUD));
        solicitud.setNombrePdfFirmado(PdfService.FIRMADO);
    }

    private PetroleraDTO petrolera(Boolean operaDispositivos) {
        PetroleraDTO dto = new PetroleraDTO();
        dto.setId(PETROLERA_ID);
        dto.setNombre("Cepsa (Moeve)");
        dto.setActiva(true);
        dto.setOperaDispositivos(operaDispositivos);
        return dto;
    }

    private CrearSolicitudDTO altaDTO() {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setTipoSolicitud(TipoSolicitud.ALTA_DISPOSITIVO);
        dto.setMatricula("1234abc");
        return dto;
    }

    private List<Path> ficherosBajoLaRaiz() throws IOException {
        try (var paths = Files.walk(raiz)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }

    // ---------- 1. transiciones desde el estado equivocado ----------

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"BORRADOR"})
    void enviarASocioSoloSeAdmiteDesdeBorrador(EstadoSolicitud estado) throws IOException {
        SolicitudDispositivo solicitud = solicitud(estado);
        conImpresoEditable(solicitud);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
        assertThat(solicitud.getRutaPdfEnviado()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"BORRADOR"})
    void guardarPdfEditadoSoloSeAdmiteDesdeBorrador(EstadoSolicitud estado) throws IOException {
        SolicitudDispositivo solicitud = solicitud(estado);
        conImpresoEditable(solicitud);
        String rutaOriginal = solicitud.getRutaPdfEditable();

        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, ficheroPdf("otro.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfEditable()).isEqualTo(rutaOriginal);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"ENVIADO_SOCIO"})
    void subirPdfFirmadoSoloSeAdmiteConElImpresoEnManosDelSocio(EstadoSolicitud estado) throws IOException {
        SolicitudDispositivo solicitud = solicitud(estado);

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("firmado.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
        assertThat(solicitud.getFechaRecepcionFirmado()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"ENVIADO_SOCIO"})
    void aceptarFirmaSocioSoloSeAdmiteDesdeEnviadoSocio(EstadoSolicitud estado) throws IOException {
        SolicitudDispositivo solicitud = solicitud(estado);
        conImpresoFirmado(solicitud);

        assertThatThrownBy(() -> service.aceptarFirmaSocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
    }

    /**
     * PENDIENTE queda fuera porque es el estado heredado: una solicitud parada ahi se sigue
     * presentando a la petrolera por el camino antiguo, sin impreso adjunto.
     */
    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"FIRMADO_SOCIO", "PENDIENTE"})
    void enviarAPetroleraSoloSeAdmiteConLaFirmaAceptada(EstadoSolicitud estado) throws IOException {
        SolicitudDispositivo solicitud = solicitud(estado);
        conImpresoFirmado(solicitud);

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
        assertThat(solicitud.getRutaPdfFinal()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"ENVIADO_PETROLERA"})
    void responderPetroleraSoloSeAdmiteConLaSolicitudPresentada(EstadoSolicitud estado) {
        SolicitudDispositivo solicitud = solicitud(estado);

        assertThatThrownBy(() -> service.responderPetrolera(SOLICITUD_ID, true, "Ok", null))
                .isInstanceOf(RuntimeException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
        verify(dispositivoService, never()).crearDispositivo(anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    void notificarSocioDesdeBorradorNoSeAdmite() {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);

        assertThatThrownBy(() -> service.notificarSocio(SOLICITUD_ID))
                .isInstanceOf(RuntimeException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
    }

    // ---------- 2. la misma transicion dos veces seguidas ----------

    @Test
    void enviarASocioDosVecesNoDuplicaElCorreo() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);

        service.enviarASocio(SOLICITUD_ID);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void enviarAPetroleraDosVecesNoDuplicaLaPresentacion() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);

        service.enviarAPetrolera(SOLICITUD_ID);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        verify(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(),
                anyMap(), any(Path.class), anyString());
    }

    @Test
    void responderDosVecesNoCreaDosDispositivos() {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_PETROLERA);
        Dispositivo nuevo = new Dispositivo();
        nuevo.setId(DISPOSITIVO_ID);
        when(dispositivoService.crearDispositivo(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(nuevo);

        service.responderPetrolera(SOLICITUD_ID, true, "Aprobado", null);

        assertThatThrownBy(() -> service.responderPetrolera(SOLICITUD_ID, true, "Aprobado", null))
                .isInstanceOf(RuntimeException.class);

        verify(dispositivoService).crearDispositivo(anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    void reSubirElImpresoFirmadoCuandoLaSolicitudYaAvanzoNoSeAdmite() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("firmado.pdf"));
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        String rutaTrasElPrimerEnvio = solicitud.getRutaPdfFirmado();

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("otro.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isEqualTo(rutaTrasElPrimerEnvio);
    }

    // ---------- 3. estado heredado PENDIENTE ----------

    @Test
    void unaSolicitudHeredadaSinNumeroNoEntraEnElCircuitoDeFirma() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        solicitud.setNumeroSolicitud(null);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("no tiene numero asignado");
        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, ficheroPdf("x.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        solicitud.setEstado(EstadoSolicitud.ENVIADO_SOCIO);
        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("x.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void unaSolicitudHeredadaSeCompletaEnteraSinNingunNullPointer() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setNumeroSolicitud(null);
        Dispositivo nuevo = new Dispositivo();
        nuevo.setId(DISPOSITIVO_ID);
        when(dispositivoService.crearDispositivo(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(nuevo);

        SolicitudDispositivoDTO presentada = service.enviarAPetrolera(SOLICITUD_ID);
        assertThat(presentada.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);

        SolicitudDispositivoDTO respondida = service.responderPetrolera(SOLICITUD_ID, true, "Ok", null);
        assertThat(respondida.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADO);
        assertThat(respondida.getNumeroSolicitud()).isNull();
    }

    @Test
    void unaSolicitudHeredadaPresentadaDosVecesNoSeReenvia() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setNumeroSolicitud(null);

        service.enviarAPetrolera(SOLICITUD_ID);

        // Ya no esta en PENDIENTE: deja de ser heredada y el circuito normal la rechaza
        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        verify(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void unaSolicitudHeredadaSeSigueLeyendoSinNumero() {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setNumeroSolicitud(null);

        SolicitudDispositivoDTO leida = service.obtenerPorId(SOLICITUD_ID);

        assertThat(leida.getNumeroSolicitud()).isNull();
        assertThat(leida.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    // ---------- 4. ficheros hostiles ----------

    @Test
    void elImpresoFirmadoNoPuedeEscaparDelDirectorioDeLaSolicitud() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("../../../../etc/passwd"));

        Path directorioSolicitud = almacen.resolve(NUMERO_SOLICITUD);
        assertThat(Path.of(solicitud.getRutaPdfFirmado()).normalize())
                .isEqualTo(directorioSolicitud.resolve(PdfService.FIRMADO));
        assertThat(ficherosBajoLaRaiz())
                .allSatisfy(fichero -> assertThat(fichero.normalize()).startsWith(directorioSolicitud));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "/etc/shadow",
            "C:\\Windows\\win.ini",
            "firmado\u0000.pdf",
            "impreso fïrmado ñ €.pdf"
    })
    void elNombreDeclaradoSeGuardaSaneadoYSinRutas(String nombreOriginal) throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf(nombreOriginal));

        assertThat(solicitud.getNombrePdfFirmado())
                .doesNotContain("/")
                .doesNotContain("\\")
                .doesNotContain("..")
                .doesNotContain("\u0000")
                .isNotBlank();
    }

    @Test
    void unNombreDeFicheroDesmesuradoNoDesbordaLaColumna() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, ficheroPdf("a".repeat(400) + ".pdf"));

        assertThat(solicitud.getNombrePdfFirmado()).hasSizeLessThanOrEqualTo(255);
    }

    @Test
    void unImpresoFirmadoVacioNoSeAdmite() {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        MockMultipartFile vacio = new MockMultipartFile("file", "firmado.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, vacio))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
    }

    @Test
    void unEjecutableDisfrazadoDePdfNoSeAdmiteComoImpresoFirmado() {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        MockMultipartFile falso = new MockMultipartFile("file", "firmado.pdf", "application/pdf",
                "MZ\u0090\u0000no soy un pdf".getBytes(StandardCharsets.ISO_8859_1));

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, falso))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
    }

    @Test
    void unImpresoEditadoQueNoEsPdfNoDestruyeElImpresoDescargado() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        Path editable = Path.of(solicitud.getRutaPdfEditable());
        byte[] contenidoBueno = Files.readAllBytes(editable);

        MockMultipartFile basura = new MockMultipartFile("file", "editado.pdf", "application/pdf",
                "esto no es un pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, basura))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(Files.readAllBytes(editable)).isEqualTo(contenidoBueno);
    }

    // ---------- 5. el PDF desaparece del disco entre dos pasos ----------

    @Test
    void siElImpresoFirmadoDesapareceAceptarLaFirmaNoCambiaElEstado() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        conImpresoFirmado(solicitud);
        Files.delete(Path.of(solicitud.getRutaPdfFirmado()));

        assertThatThrownBy(() -> service.aceptarFirmaSocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void siElImpresoFirmadoDesapareceNoSePresentaALaPetrolera() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        Files.delete(Path.of(solicitud.getRutaPdfFirmado()));

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
        assertThat(solicitud.getRutaPdfFinal()).isNull();
        assertThat(solicitud.getFechaEnvioPetrolera()).isNull();
    }

    @Test
    void siElImpresoEditableDesapareceNoSeEnviaAlSocio() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        Files.delete(Path.of(solicitud.getRutaPdfEditable()));

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(solicitud.getFechaEnvioSocio()).isNull();
    }

    // ---------- 6. fallos de los servicios externos ----------

    @Test
    void unaPlantillaDeCeroBytesNoLlegaACrearLaSolicitud() throws IOException {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(true));
        when(dispositivoService.validarMatriculaDisponible("1234abc")).thenReturn(true);
        when(petrolerasClient.obtenerPlantillaDocumento(anyLong(), any(TipoSolicitud.class)))
                .thenReturn(new byte[0]);

        assertThatThrownBy(() -> service.crear(altaDTO()))
                .isInstanceOf(BusinessValidationException.class);

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }

    @Test
    void siPetrolerasRespondeConUnErrorLaSolicitudNoNace() throws IOException {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(true));
        when(dispositivoService.validarMatriculaDisponible("1234abc")).thenReturn(true);
        when(petrolerasClient.obtenerPlantillaDocumento(anyLong(), any(TipoSolicitud.class)))
                .thenThrow(new IOException("500 Internal Server Error"));

        assertThatThrownBy(() -> service.crear(altaDTO()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("No se pudo obtener el impreso");

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }

    @Test
    void siElServicioDeSociosEstaCaidoElEnvioAlSocioSeCompletaIgual() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        when(restTemplate.getForObject(eq(SOCIOS_URL + "/api/socios/" + SOCIO_ID), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        SolicitudDispositivoDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void unSocioSinCorreoNoImpideQueElImpresoSalgaDeBorrador() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        Map<String, Object> socioSinEmail = new HashMap<>();
        socioSinEmail.put("nombre", "Transportes Perez");
        socioSinEmail.put("email", null);
        when(restTemplate.getForObject(eq(SOCIOS_URL + "/api/socios/" + SOCIO_ID), eq(Map.class)))
                .thenReturn(socioSinEmail);

        SolicitudDispositivoDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void siElServidorDeCorreoFallaElEnvioAlSocioNoSeDeshaceYQuedaEnElHistorial() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        doThrow(new RuntimeException("SMTP 421 service not available"))
                .when(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(),
                        anyMap(), any(Path.class), anyString());

        SolicitudDispositivoDTO resultado = service.enviarASocio(SOLICITUD_ID);

        // La etapa ya ha avanzado y el impreso esta aplanado: un fallo de correo no lo deshace
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(solicitud.getFechaEnvioSocio()).isNotNull();
        assertThat(solicitud.getCorreosEnviados()).contains("DOCUMENTO_SOCIO_DISPOSITIVO");
        assertThat(solicitud.getCorreosEnviados()).contains("socio@example.com");
        assertThat(solicitud.getCorreosEnviados()).contains("SMTP 421");
    }

    @Test
    void siElServidorDeCorreoFallaLaPresentacionALaPetroleraNoSeDeshace() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        doThrow(new RuntimeException("SMTP 421 service not available"))
                .when(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(),
                        anyMap(), any(Path.class), anyString());

        SolicitudDispositivoDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
        assertThat(solicitud.getCorreosEnviados()).contains("SMTP 421");
    }

    @Test
    void unaPetroleraSinCorreoNoDeshaceLaPresentacion() throws IOException {
        SolicitudDispositivo solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        Map<String, Object> petroleraSinEmail = new HashMap<>();
        petroleraSinEmail.put("nombre", "Cepsa (Moeve)");
        petroleraSinEmail.put("email", null);
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(Map.class))).thenReturn(petroleraSinEmail);

        SolicitudDispositivoDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_PETROLERA);
        // Se cae al buzon de ATG antes que perder la presentacion
        verify(emailService).enviarCorreoConPlantillaYAdjunto(eq("admin@atg.com"), anyString(),
                anyString(), anyMap(), any(Path.class), anyString());
    }

    // ---------- 7. datos hostiles ----------

    /**
     * La matricula viaja a una columna de 20. Sin limite declarado en el DTO el desbordamiento
     * no aparece hasta el flush, y el operador recibe un error interno en vez de un aviso.
     */
    @Test
    void unaMatriculaMasLargaQueSuColumnaNoPasaLaValidacionDelDTO() {
        CrearSolicitudDTO dto = altaDTO();
        dto.setMatricula("X".repeat(21));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(dto))
                    .anySatisfy(violacion ->
                            assertThat(violacion.getPropertyPath()).hasToString("matricula"));
        }
    }

    @Test
    void unaMatriculaJustoEnElLimiteSiSeAdmite() {
        CrearSolicitudDTO dto = altaDTO();
        dto.setMatricula("X".repeat(20));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(dto)).isEmpty();
        }
    }

    @Test
    void unaSolicitudDeCreditoQueApuntaAUnDispositivoInexistenteNoSeCrea() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(true));
        CrearSolicitudDTO dto = altaDTO();
        dto.setTipoSolicitud(TipoSolicitud.SOLICITUD_CREDITO);
        dto.setDispositivoId(999L);
        dto.setMonto(new BigDecimal("100.00"));
        when(dispositivoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dispositivo no encontrado");

        verify(solicitudRepository, never()).save(any(SolicitudDispositivo.class));
    }
}
