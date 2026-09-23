package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.client.PetrolerasClient;
import com.manuhd.app.tarjetas.dto.CrearSolicitudDTO;
import com.manuhd.app.tarjetas.dto.EnvioCorreoResult;
import com.manuhd.app.tarjetas.dto.MarcarEntregadaDTO;
import com.manuhd.app.tarjetas.dto.PetroleraDTO;
import com.manuhd.app.tarjetas.dto.SocioDTO;
import com.manuhd.app.tarjetas.dto.SolicitudTarjetaDTO;
import com.manuhd.app.tarjetas.exception.BusinessValidationException;
import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.PlantillaTarjeta;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import com.manuhd.app.tarjetas.security.UsuarioActualService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas adversariales del circuito del documento firmado: invocar cada transicion desde
 * el estado equivocado, repetirla, subir ficheros que no son impresos, borrar el PDF entre
 * dos pasos y dejar caer los servicios externos.
 *
 * Son deliberadamente hostiles: lo que se comprueba no es el camino feliz (ya cubierto en
 * {@link SolicitudTarjetaServiceTest}) sino que el circuito no avanza de estado, no crea
 * tarjetas de mas y no escribe fuera del directorio de la solicitud cuando se le maltrata.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudTarjetaCircuitoAdversarialTest {

    private static final Long SOLICITUD_ID = 1L;
    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final String NUMERO_SOLICITUD = "TAR-2026-00001";
    private static final String EMAIL_SOCIO = "socio@example.com";

    @Mock
    private SolicitudTarjetaRepository repository;

    @Mock
    private PlantillaTarjetaService plantillaService;

    @Mock
    private EmailService emailService;

    @Mock
    private TarjetaService tarjetaService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PetrolerasClient petrolerasClient;

    @Spy
    private UsuarioActualService usuarioActual = new UsuarioActualService();

    @Spy
    private PdfService pdfService = new PdfService();

    @InjectMocks
    private SolicitudTarjetaService service;

    /** Raiz de trabajo: dentro vive el almacen, y al lado el "fuera" que nadie debe tocar. */
    @TempDir
    Path raiz;

    private Path almacen;
    private SocioDTO socio;
    private PetroleraDTO petrolera;

    @BeforeEach
    void setUp() throws IOException {
        almacen = Files.createDirectories(raiz.resolve("storage"));
        ReflectionTestUtils.setField(pdfService, "tarjetasPath", almacen.toString());

        socio = new SocioDTO(SOCIO_ID, "Transportes Ejemplo SL", EMAIL_SOCIO, "600111222",
                "Calle Mayor 1", "Alcala de Henares", "28801", "Madrid", "S-001");
        petrolera = new PetroleraDTO(PETROLERA_ID, "Repsol", "petrolera@example.com");

        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("preferred_username", "cristina")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
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

    private MockMultipartFile multipartPdf(String nombre) throws IOException {
        return new MockMultipartFile("file", nombre, "application/pdf", pdfDeUnaPagina());
    }

    private SolicitudTarjeta solicitud(EstadoSolicitud estado) {
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setId(SOLICITUD_ID);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setMatricula("1234ABC");
        solicitud.setTipo(TipoSolicitud.ALTA);
        solicitud.setEstado(estado);
        solicitud.setNumeroSolicitud(NUMERO_SOLICITUD);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        when(repository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));
        return solicitud;
    }

    private void conImpresoEditable(SolicitudTarjeta solicitud) throws IOException {
        solicitud.setRutaPdfEditable(
                pdfService.copiarPlantillaParaSolicitud(pdfDeUnaPagina(), NUMERO_SOLICITUD));
        solicitud.setNombrePdfEditable(PdfService.EDITABLE);
    }

    private void conImpresoFirmado(SolicitudTarjeta solicitud) throws IOException {
        solicitud.setRutaPdfFirmado(
                pdfService.guardarPdfFirmado(multipartPdf("firmado.pdf"), NUMERO_SOLICITUD));
        solicitud.setNombrePdfFirmado(PdfService.FIRMADO);
    }

    private void mockServiciosExternos() {
        when(restTemplate.getForObject(contains("/api/socios/"), eq(SocioDTO.class))).thenReturn(socio);
        when(restTemplate.getForObject(contains("/api/petroleras/"), eq(PetroleraDTO.class))).thenReturn(petrolera);
    }

    private void mockPlantillaConAdjunto(TipoPlantilla tipo) {
        PlantillaTarjeta plantilla = new PlantillaTarjeta();
        plantilla.setTipo(tipo);
        plantilla.setAsunto("Asunto " + tipo);
        plantilla.setCuerpo("Cuerpo " + tipo);
        plantilla.setActiva(true);
        when(plantillaService.buscarPlantillaActiva(tipo)).thenReturn(Optional.of(plantilla));
        when(emailService.enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(tipo.name()), any(Path.class), anyString()))
                .thenReturn(new EnvioCorreoResult(true, tipo.name(), EMAIL_SOCIO));
    }

    private CrearSolicitudDTO crearAltaDTO() {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setMatricula("1234ABC");
        dto.setTipo(TipoSolicitud.ALTA);
        return dto;
    }

    /** Todos los ficheros que existen hoy bajo la raiz temporal. */
    private List<Path> ficherosBajoLaRaiz() throws IOException {
        try (var paths = Files.walk(raiz)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }

    // ---------- 1. transiciones desde el estado equivocado ----------

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"BORRADOR"})
    void enviarASocioSoloSeAdmiteDesdeBorrador(EstadoSolicitud estado) throws IOException {
        SolicitudTarjeta solicitud = solicitud(estado);
        conImpresoEditable(solicitud);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
        assertThat(solicitud.getRutaPdfEnviado()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"BORRADOR"})
    void guardarPdfEditadoSoloSeAdmiteDesdeBorrador(EstadoSolicitud estado) throws IOException {
        SolicitudTarjeta solicitud = solicitud(estado);
        conImpresoEditable(solicitud);
        String rutaOriginal = solicitud.getRutaPdfEditable();

        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, multipartPdf("otro.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfEditable()).isEqualTo(rutaOriginal);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"ENVIADO_SOCIO"})
    void subirPdfFirmadoSoloSeAdmiteConElImpresoEnManosDelSocio(EstadoSolicitud estado) throws IOException {
        SolicitudTarjeta solicitud = solicitud(estado);

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("firmado.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
        assertThat(solicitud.getFechaRecepcionFirmado()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"ENVIADO_SOCIO"})
    void aceptarFirmaSocioSoloSeAdmiteDesdeEnviadoSocio(EstadoSolicitud estado) throws IOException {
        SolicitudTarjeta solicitud = solicitud(estado);
        conImpresoFirmado(solicitud);

        assertThatThrownBy(() -> service.aceptarFirmaSocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EnumSource.Mode.EXCLUDE, names = {"FIRMADO_SOCIO"})
    void enviarAPetroleraSoloSeAdmiteConLaFirmaAceptada(EstadoSolicitud estado) throws IOException {
        SolicitudTarjeta solicitud = solicitud(estado);
        conImpresoFirmado(solicitud);

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(estado);
        assertThat(solicitud.getRutaPdfFinal()).isNull();
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(),
                any(), anyString(), any(Path.class), anyString());
    }

    @Test
    void aprobarPorPetroleraDesdeBorradorNoSeAdmite() {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);

        assertThatThrownBy(() -> service.aprobarPorPetrolera(SOLICITUD_ID))
                .isInstanceOf(RuntimeException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
    }

    @Test
    void marcarEntregadaDesdeBorradorNiCompletaNiCreaTarjeta() {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);

        assertThatThrownBy(() -> service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO()))
                .isInstanceOf(RuntimeException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        verify(tarjetaService, never()).create(any());
    }

    // ---------- 2. la misma transicion dos veces seguidas ----------

    @Test
    void enviarASocioDosVecesNoDuplicaElCorreoNiVuelveAAplanar() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        mockServiciosExternos();
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_SOCIO);

        service.enviarASocio(SOLICITUD_ID);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        verify(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(TipoPlantilla.DOCUMENTO_SOCIO.name()), any(Path.class), anyString());
    }

    @Test
    void enviarAPetroleraDosVecesNoDuplicaLaPresentacion() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        mockServiciosExternos();
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_PETROLERA);

        service.enviarAPetrolera(SOLICITUD_ID);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        verify(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(TipoPlantilla.DOCUMENTO_PETROLERA.name()), any(Path.class), anyString());
    }

    @Test
    void marcarEntregadaDosVecesNoCreaDosTarjetas() {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.TARJETA_LLEGADA);

        service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO());

        assertThatThrownBy(() -> service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(tarjetaService).create(any());
    }

    @Test
    void reSubirElImpresoFirmadoCuandoLaSolicitudYaAvanzoNoSeAdmite() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("firmado.pdf"));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        String rutaTrasElPrimerEnvio = solicitud.getRutaPdfFirmado();

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("otro-firmado.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isEqualTo(rutaTrasElPrimerEnvio);
    }

    // ---------- 3. solicitudes heredadas (sin numero de solicitud) ----------

    @Test
    void unaSolicitudSinNumeroNoEntraEnNingunPasoDelCircuito() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
        solicitud.setNumeroSolicitud(null);

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("no tiene número asignado");
        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, multipartPdf("x.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        solicitud.setEstado(EstadoSolicitud.ENVIADO_SOCIO);
        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("x.pdf")))
                .isInstanceOf(BusinessValidationException.class);

        solicitud.setEstado(EstadoSolicitud.FIRMADO_SOCIO);
        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
    }

    @Test
    void unaSolicitudHeredadaSinNumeroSigueCompletandosePorElCaminoAntiguo() {
        // Fila anterior al circuito: sin numero y sin ningun PDF, parada en PENDIENTE.
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setNumeroSolicitud(null);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.ALTA_APROBADA)).thenReturn(Optional.empty());

        SolicitudTarjetaDTO aprobada = service.aprobarPorPetrolera(SOLICITUD_ID);
        assertThat(aprobada.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);

        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        SolicitudTarjetaDTO entregada = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO());
        assertThat(entregada.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
    }

    // ---------- 4. ficheros hostiles ----------

    @Test
    void elNombreDelImpresoFirmadoNoPuedeEscaparDelDirectorioDeLaSolicitud() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("../../../../etc/passwd"));

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
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);

        service.subirPdfFirmado(SOLICITUD_ID, multipartPdf(nombreOriginal));

        assertThat(solicitud.getNombrePdfFirmado())
                .doesNotContain("/")
                .doesNotContain("\\")
                .doesNotContain("..")
                .doesNotContain("\u0000")
                .isNotBlank();
    }

    @Test
    void unNombreDeFicheroDesmesuradoNoDesbordaLaColumna() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        String nombreLargo = "a".repeat(400) + ".pdf";

        service.subirPdfFirmado(SOLICITUD_ID, multipartPdf(nombreLargo));

        assertThat(solicitud.getNombrePdfFirmado()).hasSizeLessThanOrEqualTo(255);
    }

    @Test
    void unImpresoFirmadoVacioNoSeAdmite() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        MockMultipartFile vacio = new MockMultipartFile("file", "firmado.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, vacio))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
        assertThat(solicitud.getFechaRecepcionFirmado()).isNull();
    }

    @Test
    void unEjecutableDisfrazadoDePdfNoSeAdmiteComoImpresoFirmado() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        MockMultipartFile falso = new MockMultipartFile("file", "firmado.pdf", "application/pdf",
                "MZ\u0090\u0000no soy un pdf".getBytes(StandardCharsets.ISO_8859_1));

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, falso))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getRutaPdfFirmado()).isNull();
    }

    @Test
    void unImpresoEditadoQueNoEsPdfNoDestruyeElImpresoDescargado() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        Path editable = Path.of(solicitud.getRutaPdfEditable());
        byte[] contenidoBueno = Files.readAllBytes(editable);

        MockMultipartFile basura = new MockMultipartFile("file", "editado.pdf", "application/pdf",
                "esto no es un pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.guardarPdfEditado(SOLICITUD_ID, basura))
                .isInstanceOf(BusinessValidationException.class);

        // El impreso descargado de la petrolera sigue intacto: la solicitud no queda inservible
        assertThat(Files.readAllBytes(editable)).isEqualTo(contenidoBueno);
    }

    // ---------- 5. el PDF desaparece del disco entre dos pasos ----------

    @Test
    void siElImpresoFirmadoDesapareceAceptarLaFirmaNoCambiaElEstado() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.ENVIADO_SOCIO);
        conImpresoFirmado(solicitud);
        Files.delete(Path.of(solicitud.getRutaPdfFirmado()));

        assertThatThrownBy(() -> service.aceptarFirmaSocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void siElImpresoFirmadoDesapareceNoSePresentaALaPetrolera() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
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
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
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
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenReturn(new byte[0]);
        when(repository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);
        when(repository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.create(crearAltaDTO()))
                .isInstanceOf(BusinessValidationException.class);

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void siPetrolerasRespondeConUnErrorLaSolicitudNoNace() throws IOException {
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenThrow(new IOException("500 Internal Server Error"));
        when(repository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.create(crearAltaDTO()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("No se pudo obtener el impreso");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void siElServicioDeSociosEstaCaidoElEnvioAlSocioSeCompletaIgual() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        when(restTemplate.getForObject(contains("/api/socios/"), eq(SocioDTO.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        SolicitudTarjetaDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.DOCUMENTO_SOCIO.name());
    }

    @Test
    void unSocioSinCorreoNoImpideQueElImpresoSalgaDeBorrador() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.BORRADOR);
        conImpresoEditable(solicitud);
        socio.setEmail(null);
        mockServiciosExternos();
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_SOCIO);

        SolicitudTarjetaDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
    }

    @Test
    void siElServidorDeCorreoFallaLaPresentacionALaPetroleraNoSeDeshace() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.DOCUMENTO_PETROLERA))
                .thenReturn(Optional.empty());
        when(emailService.enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString()))
                .thenThrow(new RuntimeException("SMTP 421 service not available"));

        SolicitudTarjetaDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getCorreosEnviados())
                .contains(TipoPlantilla.DOCUMENTO_PETROLERA.name())
                .contains("SMTP 421");
    }

    @Test
    void unaPetroleraSinCorreoDejaConstanciaDelFalloPeroNoDeshaceLaPresentacion() throws IOException {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.FIRMADO_SOCIO);
        conImpresoFirmado(solicitud);
        petrolera.setEmail(null);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.DOCUMENTO_PETROLERA))
                .thenReturn(Optional.empty());
        when(emailService.enviarCorreoConPlantillaYAdjunto(any(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString()))
                .thenReturn(new EnvioCorreoResult(false, TipoPlantilla.DOCUMENTO_PETROLERA.name(),
                        "petrolera", "Destinatario vacio"));

        SolicitudTarjetaDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getCorreosEnviados()).contains("Destinatario vacio");
    }

    // ---------- 7. datos hostiles ----------

    @Test
    void noSeCreaUnaSolicitudParaUnaPetroleraQueNoOperaConTarjetas() throws IOException {
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
        when(repository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);
        petrolera.setOperaTarjetas(false);
        mockServiciosExternos();

        assertThatThrownBy(() -> service.create(crearAltaDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no opera con tarjetas");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void unaPetroleraSinElFlagConfiguradoSigueAdmitiendoSolicitudes() throws IOException {
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
        when(repository.findMaxNumeroSolicitudByYear(anyString())).thenReturn(null);
        when(repository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));
        petrolera.setOperaTarjetas(null);
        mockServiciosExternos();
        PlantillaTarjeta altaSocio = new PlantillaTarjeta();
        altaSocio.setTipo(TipoPlantilla.ALTA_SOCIO);
        altaSocio.setAsunto("Asunto");
        altaSocio.setCuerpo("Cuerpo");
        when(plantillaService.obtenerPlantillaActiva(TipoPlantilla.ALTA_SOCIO)).thenReturn(altaSocio);
        when(emailService.enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new EnvioCorreoResult(true, TipoPlantilla.ALTA_SOCIO.name(), EMAIL_SOCIO));

        SolicitudTarjetaDTO creada = service.create(crearAltaDTO());

        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
    }

    @Test
    void unaSolicitudQueApuntaAUnaTarjetaInexistenteNoSeCompleta() {
        SolicitudTarjeta solicitud = solicitud(EstadoSolicitud.TARJETA_LLEGADA);
        solicitud.setTipo(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(999L);
        when(tarjetaService.findById(999L)).thenThrow(new RuntimeException("Tarjeta no encontrada con id: 999"));

        assertThatThrownBy(() -> service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarjeta no encontrada");
    }
}
