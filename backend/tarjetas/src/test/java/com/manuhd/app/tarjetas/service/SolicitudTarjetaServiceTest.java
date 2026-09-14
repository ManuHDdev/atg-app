package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.client.PetrolerasClient;
import com.manuhd.app.tarjetas.dto.AprobarBajaDTO;
import com.manuhd.app.tarjetas.dto.AprobarDuplicadoDTO;
import com.manuhd.app.tarjetas.dto.CrearSolicitudDTO;
import com.manuhd.app.tarjetas.dto.EnvioCorreoResult;
import com.manuhd.app.tarjetas.dto.MarcarEntregadaDTO;
import com.manuhd.app.tarjetas.dto.PetroleraDTO;
import com.manuhd.app.tarjetas.dto.RegistrarLlegadaDTO;
import com.manuhd.app.tarjetas.dto.SocioDTO;
import com.manuhd.app.tarjetas.dto.SolicitudTarjetaDTO;
import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.MotivoDuplicado;
import com.manuhd.app.tarjetas.model.PlantillaTarjeta;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.Tarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import com.manuhd.app.tarjetas.exception.BusinessValidationException;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de los correos y transiciones de estado de las solicitudes de tarjeta.
 * Sin contexto de Spring ni base de datos: solo JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SolicitudTarjetaServiceTest {

    private static final Long SOLICITUD_ID = 1L;
    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final Long TARJETA_ID = 30L;
    private static final String EMAIL_SOCIO = "socio@example.com";
    private static final String USUARIO_TOKEN = "cristina";

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

    // Colaborador real: los tests ejercitan la lectura del JWT, no un doble de prueba.
    @Spy
    private UsuarioActualService usuarioActual = new UsuarioActualService();

    // Colaborador real sobre un directorio temporal: el circuito del documento firmado se
    // apoya en ficheros de verdad, y comprobar que existen es la mitad de lo que se valida.
    @Spy
    private PdfService pdfService = new PdfService();

    @InjectMocks
    private SolicitudTarjetaService service;

    @TempDir
    Path storageTarjetas;

    private SocioDTO socio;
    private PetroleraDTO petrolera;

    @BeforeEach
    void setUp() {
        socio = new SocioDTO(SOCIO_ID, "Transportes Ejemplo SL", EMAIL_SOCIO, "600111222",
                "Calle Mayor 1", "Alcalá de Henares", "28801", "Madrid", "S-001");
        petrolera = new PetroleraDTO(PETROLERA_ID, "Repsol", "petrolera@example.com");
        ReflectionTestUtils.setField(pdfService, "tarjetasPath", storageTarjetas.toString());
        autenticarComo(USUARIO_TOKEN);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    /** Deja en el contexto un token de Keycloak con el preferred_username indicado. */
    private void autenticarComo(String preferredUsername) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("preferred_username", preferredUsername)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    // ---------- helpers ----------

    private SolicitudTarjeta solicitud(TipoSolicitud tipo) {
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setId(SOLICITUD_ID);
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setPetroleraId(PETROLERA_ID);
        solicitud.setMatricula("1234ABC");
        solicitud.setNumeroContrato("CTR-9876");
        solicitud.setTipo(tipo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return solicitud;
    }

    private PlantillaTarjeta plantilla(TipoPlantilla tipo) {
        PlantillaTarjeta plantilla = new PlantillaTarjeta();
        plantilla.setTipo(tipo);
        plantilla.setAsunto("Asunto " + tipo);
        plantilla.setCuerpo("Cuerpo " + tipo);
        plantilla.setActiva(true);
        return plantilla;
    }

    private void mockSolicitudGuardada(SolicitudTarjeta solicitud) {
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        when(repository.save(any(SolicitudTarjeta.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void mockServiciosExternos() {
        when(restTemplate.getForObject(contains("/api/socios/"), eq(SocioDTO.class))).thenReturn(socio);
        when(restTemplate.getForObject(contains("/api/petroleras/"), eq(PetroleraDTO.class))).thenReturn(petrolera);
    }

    private void mockPlantillaActiva(TipoPlantilla tipo) {
        when(plantillaService.buscarPlantillaActiva(tipo)).thenReturn(Optional.of(plantilla(tipo)));
        when(emailService.enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), eq(tipo.name())))
                .thenReturn(new EnvioCorreoResult(true, tipo.name(), EMAIL_SOCIO));
    }

    /** Plantilla obligatoria (obtenerPlantillaActiva): los correos de llegada y de alta. */
    private void mockPlantillaObligatoria(TipoPlantilla tipo) {
        when(plantillaService.obtenerPlantillaActiva(tipo)).thenReturn(plantilla(tipo));
        when(emailService.enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), eq(tipo.name())))
                .thenReturn(new EnvioCorreoResult(true, tipo.name(), EMAIL_SOCIO));
    }

    /** Alta de solicitud: create() guarda la solicitud y despues su registro de correos. */
    private void mockGuardadoDeNuevaSolicitud() {
        when(repository.save(any(SolicitudTarjeta.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CrearSolicitudDTO crearDTO(TipoSolicitud tipo, LocalDate fechaLlegadaEstimada) {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setMatricula("1234ABC");
        dto.setNumeroContrato("CTR-9876");
        dto.setTipo(tipo);
        dto.setFechaLlegadaEstimada(fechaLlegadaEstimada);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> capturarVariables(TipoPlantilla tipo) {
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(),
                captor.capture(), eq(tipo.name()));
        return captor.getValue();
    }

    // ---------- helpers del circuito del documento firmado ----------

    private static final String NUMERO_SOLICITUD = "TAR-2026-00001";

    /** Solicitud ya dentro del circuito: tiene número, y por tanto directorio propio en disco. */
    private SolicitudTarjeta solicitudEnCircuito(TipoSolicitud tipo, EstadoSolicitud estado) {
        SolicitudTarjeta solicitud = solicitud(tipo);
        solicitud.setNumeroSolicitud(NUMERO_SOLICITUD);
        solicitud.setEstado(estado);
        return solicitud;
    }

    /** PDF mínimo pero válido: PDFBox tiene que poder abrirlo para aplanarlo. */
    private byte[] pdfDeUnaPagina() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** Deja un PDF real en el directorio de la solicitud y devuelve su ruta. */
    private String pdfEnDisco(String nombre) throws IOException {
        Path directorio = storageTarjetas.resolve(NUMERO_SOLICITUD);
        Files.createDirectories(directorio);
        Path ruta = directorio.resolve(nombre);
        Files.write(ruta, pdfDeUnaPagina());
        return ruta.toString();
    }

    private MockMultipartFile multipartPdf(String nombre) throws IOException {
        return new MockMultipartFile("file", nombre, "application/pdf", pdfDeUnaPagina());
    }

    /** La petrolera sí tiene impreso configurado para el tipo de solicitud. */
    private void mockPlantillaDocumentoDescargable() throws IOException {
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
    }

    private void mockPlantillaConAdjunto(TipoPlantilla tipo) {
        when(plantillaService.buscarPlantillaActiva(tipo)).thenReturn(Optional.of(plantilla(tipo)));
        when(emailService.enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(tipo.name()), any(Path.class), anyString()))
                .thenReturn(new EnvioCorreoResult(true, tipo.name(), EMAIL_SOCIO));
    }

    /** Ruta del PDF que se ha adjuntado realmente al correo del tipo indicado. */
    private Path capturarAdjunto(TipoPlantilla tipo) {
        ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);
        verify(emailService).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(tipo.name()), captor.capture(), anyString());
        return captor.getValue();
    }

    private Tarjeta tarjeta(int cantidad) {
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setId(TARJETA_ID);
        tarjeta.setSocioId(SOCIO_ID);
        tarjeta.setPetroleraId(PETROLERA_ID);
        tarjeta.setMatricula("1234ABC");
        tarjeta.setActiva(true);
        tarjeta.setCantidad(cantidad);
        return tarjeta;
    }

    // ---------- respuesta de la petrolera ----------

    @Test
    void aprobarPorPetroleraEnviaCorreoDeAltaAprobadaYPasaAAprobada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_APROBADA);

        SolicitudTarjetaDTO resultado = service.aprobarPorPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.ALTA_APROBADA.name()));
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.ALTA_APROBADA.name());
    }

    @Test
    void aprobarPorPetroleraSinPlantillaActivaNoRompeLaTransicionNiRegistraCorreos() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.ALTA_APROBADA)).thenReturn(Optional.empty());

        SolicitudTarjetaDTO resultado = service.aprobarPorPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(solicitud.getCorreosEnviados()).isNull();
        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
    }

    // ---------- denegación de la petrolera ----------

    @Test
    void denegarPorPetroleraEnviaCorreoDeAltaRechazadaConElMotivoYPasaARechazada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_RECHAZADA);

        SolicitudTarjetaDTO resultado = service.denegarPorPetrolera(SOLICITUD_ID, "Contrato no vigente");

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(capturarVariables(TipoPlantilla.ALTA_RECHAZADA))
                .containsEntry("motivo", "Contrato no vigente");
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.ALTA_RECHAZADA.name());
    }

    // ---------- baja / duplicado aprobados por la petrolera ----------

    @Test
    void aprobarBajaPorPetroleraEnviaBajaConfirmadaYNoBajaSocio() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.BAJA);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        mockPlantillaActiva(TipoPlantilla.BAJA_CONFIRMADA);

        AprobarBajaDTO dto = new AprobarBajaDTO(LocalDate.of(2026, 1, 15), null);
        SolicitudTarjetaDTO resultado = service.aprobarBajaPorPetrolera(SOLICITUD_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.BAJA_CONFIRMADA.name()));
        verify(plantillaService, never()).buscarPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
        verify(plantillaService, never()).obtenerPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.BAJA_CONFIRMADA.name());
    }

    /**
     * Un duplicado es una tarjeta física que todavía tiene que llegar y entregarse: la respuesta
     * de la petrolera lo deja APROBADA, no cerrado, y no toca aún la cantidad de la tarjeta.
     */
    @Test
    void aprobarDuplicadoPorPetroleraEnviaDuplicadoConfirmadaYPasaAAprobada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        mockPlantillaActiva(TipoPlantilla.DUPLICADO_CONFIRMADA);

        AprobarDuplicadoDTO dto = new AprobarDuplicadoDTO(LocalDate.of(2026, 2, 1), null);
        SolicitudTarjetaDTO resultado = service.aprobarDuplicadoPorPetrolera(SOLICITUD_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.DUPLICADO_CONFIRMADA.name()));
        verify(plantillaService, never()).obtenerPlantillaActiva(TipoPlantilla.DUPLICADO_SOCIO);
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.DUPLICADO_CONFIRMADA.name());
    }

    @Test
    void aprobarDuplicadoPorPetroleraNoIncrementaTodaviaLaCantidadDeLaTarjeta() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        Tarjeta tarjeta = tarjeta(1);
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta);
        mockPlantillaActiva(TipoPlantilla.DUPLICADO_CONFIRMADA);

        service.aprobarDuplicadoPorPetrolera(SOLICITUD_ID, new AprobarDuplicadoDTO(LocalDate.of(2026, 2, 1), null));

        assertThat(tarjeta.getCantidad()).isEqualTo(1);
        verify(tarjetaService, never()).update(any(), any(Tarjeta.class));

        // Al socio sí se le anuncia con cuántas tarjetas se quedará cuando reciba el duplicado.
        assertThat(capturarVariables(TipoPlantilla.DUPLICADO_CONFIRMADA)).containsEntry("cantidad", "2");
    }

    // ---------- regla Madrid ----------

    @ParameterizedTest
    @ValueSource(strings = {"Madrid", "  madrid  ", "MADRID ", "Comunidad de Madrid", "COMUNIDAD DE MADRÍD"})
    void registrarLlegadaUsaLaPlantillaDeMadridParaLaComunidadDeMadrid(String provincia) {
        assertThat(service.esProvinciaMadrid(provincia)).isTrue();
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_MADRID, TipoSolicitud.ALTA);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Toledo", "", "   "})
    void registrarLlegadaUsaLaPlantillaPostalFueraDeMadrid(String provincia) {
        assertThat(service.esProvinciaMadrid(provincia)).isFalse();
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_FUERA, TipoSolicitud.ALTA);
    }

    /**
     * El duplicado también llega físicamente, así que recorre el mismo aviso al socio que el
     * alta: recogida en Madrid, envío postal fuera.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Madrid", "Comunidad de Madrid"})
    void registrarLlegadaDeUnDuplicadoEnMadridAvisaDeLaRecogida(String provincia) {
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_MADRID, TipoSolicitud.DUPLICADO);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Toledo", "Cuenca"})
    void registrarLlegadaDeUnDuplicadoFueraDeMadridAvisaDelEnvioPostal(String provincia) {
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_FUERA, TipoSolicitud.DUPLICADO);
    }

    private void registrarLlegadaCon(String provincia, TipoPlantilla esperada, TipoSolicitud tipo) {
        socio.setProvincia(provincia);

        SolicitudTarjeta solicitud = solicitud(tipo);
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.obtenerPlantillaActiva(esperada)).thenReturn(plantilla(esperada));
        when(emailService.enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), eq(esperada.name())))
                .thenReturn(new EnvioCorreoResult(true, esperada.name(), EMAIL_SOCIO));

        RegistrarLlegadaDTO dto = new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null);
        SolicitudTarjetaDTO resultado = service.registrarLlegada(SOLICITUD_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(esperada.name()));
    }

    // ---------- variables de plantilla ----------

    @Test
    void lasVariablesIncluyenElNumeroDeContratoRealYLaDireccionDelSocio() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_APROBADA);

        service.aprobarPorPetrolera(SOLICITUD_ID);

        assertThat(capturarVariables(TipoPlantilla.ALTA_APROBADA))
                .containsEntry("numeroContrato", "CTR-9876")
                .containsEntry("direccion", "Calle Mayor 1")
                .containsEntry("poblacion", "Alcalá de Henares")
                .containsEntry("codigoPostal", "28801")
                .containsEntry("direccionCompleta", "Calle Mayor 1, 28801 Alcalá de Henares (Madrid)");
    }

    @Test
    void elNumeroDeContratoVacioSeExponeComoCadenaVacia() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setNumeroContrato(null);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_APROBADA);

        service.aprobarPorPetrolera(SOLICITUD_ID);

        assertThat(capturarVariables(TipoPlantilla.ALTA_APROBADA)).containsEntry("numeroContrato", "");
    }

    // ---------- trazabilidad: quien tramita sale del token ----------

    @Test
    void aprobarBajaPorPetroleraRegistraElUsuarioDelTokenComoProcesadoPor() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.BAJA);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.BAJA_CONFIRMADA)).thenReturn(Optional.empty());

        SolicitudTarjetaDTO resultado = service.aprobarBajaPorPetrolera(SOLICITUD_ID,
                new AprobarBajaDTO(LocalDate.of(2026, 1, 15), null));

        assertThat(resultado.getProcesadoPor()).isEqualTo(USUARIO_TOKEN);
        assertThat(solicitud.getProcesadoPor()).isEqualTo(USUARIO_TOKEN);
    }

    @Test
    void registrarLlegadaRegistraElUsuarioDelTokenComoProcesadoPor() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.obtenerPlantillaActiva(TipoPlantilla.LLEGADA_MADRID))
                .thenReturn(plantilla(TipoPlantilla.LLEGADA_MADRID));
        when(emailService.enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(),
                eq(TipoPlantilla.LLEGADA_MADRID.name())))
                .thenReturn(new EnvioCorreoResult(true, TipoPlantilla.LLEGADA_MADRID.name(), EMAIL_SOCIO));

        SolicitudTarjetaDTO resultado = service.registrarLlegada(SOLICITUD_ID,
                new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null));

        assertThat(resultado.getProcesadoPor()).isEqualTo(USUARIO_TOKEN);
    }

    @Test
    void marcarEntregadaRegistraElUsuarioDelTokenAunqueCambieElOperador() {
        autenticarComo("gema");

        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        SolicitudTarjetaDTO resultado = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        assertThat(resultado.getProcesadoPor()).isEqualTo("gema");
    }

    // ---------- marcarEntregada: alta de la tarjeta ----------

    @Test
    void marcarEntregadaCreaUnaUnicaTarjetaParaUnAlta() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        SolicitudTarjetaDTO resultado = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        ArgumentCaptor<Tarjeta> captor = ArgumentCaptor.forClass(Tarjeta.class);
        verify(tarjetaService, times(1)).create(captor.capture());
        assertThat(captor.getValue().getSolicitudId()).isEqualTo(SOLICITUD_ID);
        assertThat(captor.getValue().getActiva()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TipoSolicitud.class, names = {"BAJA", "LLEGADA"})
    void marcarEntregadaNoCreaTarjetaSiNoEsUnAlta(TipoSolicitud tipo) {
        SolicitudTarjeta solicitud = solicitud(tipo);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        verify(tarjetaService, never()).create(any(Tarjeta.class));
    }

    /**
     * El duplicado no crea una tarjeta nueva: suma una unidad a la existente, y lo hace al
     * entregarla, que es cuando el socio la tiene realmente en la mano.
     */
    @Test
    void marcarEntregadaDeUnDuplicadoIncrementaLaCantidadUnaSolaVezYCompleta() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);
        Tarjeta tarjeta = tarjeta(1);
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta);

        SolicitudTarjetaDTO resultado = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        assertThat(resultado.getFechaEntrega()).isNotNull();
        assertThat(tarjeta.getCantidad()).isEqualTo(2);
        verify(tarjetaService, times(1)).update(eq(TARJETA_ID), any(Tarjeta.class));
        verify(tarjetaService, never()).create(any(Tarjeta.class));
    }

    @Test
    void marcarEntregadaDeUnDuplicadoSinTarjetaAsociadaNoSeAdmite() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        MarcarEntregadaDTO dto = new MarcarEntregadaDTO(null, null);

        assertThatThrownBy(() -> service.marcarEntregada(SOLICITUD_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tarjeta asociada");

        verify(tarjetaService, never()).update(any(), any(Tarjeta.class));
    }

    // ---------- LLEGADA: registro en un solo paso ----------

    @Test
    void crearLlegadaNaceEnTarjetaLlegadaYAvisaAlSocioUnaSolaVez() {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_MADRID);

        LocalDate fechaLlegada = LocalDate.of(2026, 4, 10);
        SolicitudTarjetaDTO resultado = service.create(crearDTO(TipoSolicitud.LLEGADA, fechaLlegada));

        // Nace ya "llegada": no pasa por PENDIENTE ni espera respuesta de la petrolera.
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        assertThat(resultado.getFechaLlegadaEstimada()).isEqualTo(fechaLlegada);
        assertThat(resultado.getProcesadoPor()).isEqualTo(USUARIO_TOKEN);

        // El aviso al socio sale exactamente una vez, no dos como en el flujo anterior.
        verify(emailService, times(1)).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.LLEGADA_MADRID.name()));
        verify(emailService, times(1)).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
        assertThat(resultado.getCorreosEnviados()).contains(TipoPlantilla.LLEGADA_MADRID.name());
    }

    @Test
    void crearLlegadaFueraDeMadridUsaLaPlantillaPostal() {
        socio.setProvincia("Toledo");
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_FUERA);

        SolicitudTarjetaDTO resultado = service.create(crearDTO(TipoSolicitud.LLEGADA, LocalDate.of(2026, 4, 10)));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        verify(emailService, times(1)).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.LLEGADA_FUERA.name()));
    }

    @Test
    void crearLlegadaSinFechaNoSeAdmite() {
        CrearSolicitudDTO dto = crearDTO(TipoSolicitud.LLEGADA, null);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fecha de llegada es obligatoria");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void registrarLlegadaNoSeAplicaAUnaSolicitudDeTipoLlegada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.LLEGADA);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        RegistrarLlegadaDTO dto = new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null);

        assertThatThrownBy(() -> service.registrarLlegada(SOLICITUD_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya nace con la llegada registrada");

        // Lo importante: no se reenvia el aviso de llegada al socio.
        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
    }

    /** En una BAJA no llega ninguna tarjeta: no hay nada que avisar ni que entregar. */
    @Test
    void registrarLlegadaNoSeAplicaAUnaSolicitudDeBaja() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.BAJA);
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        RegistrarLlegadaDTO dto = new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null);

        assertThatThrownBy(() -> service.registrarLlegada(SOLICITUD_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no espera ninguna tarjeta");

        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
    }

    // ---------- DUPLICADO: el motivo es obligatorio ----------

    @Test
    void crearDuplicadoSinMotivoNoSeAdmite() {
        CrearSolicitudDTO dto = crearDTO(TipoSolicitud.DUPLICADO, null);
        dto.setTarjetaId(TARJETA_ID);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("motivo del duplicado es obligatorio");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void crearDuplicadoGuardaElMotivoYLoExponeALaPlantilla() throws IOException {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaDocumentoDescargable();
        mockPlantillaObligatoria(TipoPlantilla.DUPLICADO_SOCIO);

        CrearSolicitudDTO dto = crearDTO(TipoSolicitud.DUPLICADO, null);
        dto.setTarjetaId(TARJETA_ID);
        dto.setMotivoDuplicado(MotivoDuplicado.EXTRAVIO);

        SolicitudTarjetaDTO resultado = service.create(dto);

        // Un duplicado también lleva impreso firmado: nace en borrador, no presentado.
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(resultado.getMotivoDuplicado()).isEqualTo(MotivoDuplicado.EXTRAVIO);
        // La plantilla recibe la etiqueta legible, no el nombre del enum.
        assertThat(capturarVariables(TipoPlantilla.DUPLICADO_SOCIO)).containsEntry("motivoDuplicado", "Extravío");
    }

    @Test
    void elMotivoDelDuplicadoNoSeAplicaAOtrosTiposYLlegaVacioALaPlantilla() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_APROBADA);

        service.aprobarPorPetrolera(SOLICITUD_ID);

        assertThat(capturarVariables(TipoPlantilla.ALTA_APROBADA)).containsEntry("motivoDuplicado", "");
    }

    /** Recorrido completo del duplicado: llega y se entrega, igual que un alta. */
    @Test
    void unDuplicadoRecorreLlegadaYEntregaHastaCompletada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        solicitud.setMotivoDuplicado(MotivoDuplicado.DETERIORO);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        Tarjeta tarjeta = tarjeta(1);
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta);
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.DUPLICADO_CONFIRMADA)).thenReturn(Optional.empty());
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_MADRID);

        // 1. La petrolera confirma el duplicado: queda aprobado, pendiente de que llegue.
        assertThat(service.aprobarDuplicadoPorPetrolera(SOLICITUD_ID,
                new AprobarDuplicadoDTO(LocalDate.of(2026, 2, 1), null)).getEstado())
                .isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(tarjeta.getCantidad()).isEqualTo(1);

        // 2. Llega la tarjeta física y se avisa al socio (recogida en Madrid).
        assertThat(service.registrarLlegada(SOLICITUD_ID,
                new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null)).getEstado())
                .isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.LLEGADA_MADRID.name()));

        // 3. Se entrega: ahora sí sube la cantidad y el proceso queda cerrado.
        assertThat(service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null)).getEstado())
                .isEqualTo(EstadoSolicitud.COMPLETADA);
        assertThat(tarjeta.getCantidad()).isEqualTo(2);
        verify(tarjetaService, never()).create(any(Tarjeta.class));
    }

    // ---------- entrega: cierra el proceso ----------

    @Test
    void marcarEntregadaCompletaLaSolicitudYRegistraLaFechaDeEntrega() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        LocalDateTime fechaEntrega = LocalDateTime.of(2026, 5, 20, 11, 30);
        SolicitudTarjetaDTO resultado = service.marcarEntregada(SOLICITUD_ID,
                new MarcarEntregadaDTO(fechaEntrega, null));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        assertThat(resultado.getFechaEntrega()).isEqualTo(fechaEntrega);
    }

    @Test
    void marcarEntregadaSinFechaUsaElMomentoActual() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        LocalDateTime antes = LocalDateTime.now();
        SolicitudTarjetaDTO resultado = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        assertThat(resultado.getFechaEntrega()).isNotNull();
        assertThat(resultado.getFechaEntrega()).isAfterOrEqualTo(antes);
    }

    // ---------- circuito del documento firmado ----------

    @Test
    void crearUnAltaNoAvisaTodaviaALaPetrolera() throws IOException {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaDocumentoDescargable();
        mockPlantillaObligatoria(TipoPlantilla.ALTA_SOCIO);

        service.create(crearDTO(TipoSolicitud.ALTA, null));

        // La solicitud nace en BORRADOR: a la petrolera se le escribe en enviarAPetrolera(),
        // cuando ya existe el documento firmado por el socio, no antes.
        verify(emailService, never()).enviarCorreoConPlantilla(
                anyString(), anyString(), anyString(), any(), eq(TipoPlantilla.ALTA_PETROLERA.name()));
    }

    @Test
    void crearAltaNaceEnBorradorConElImpresoDeLaPetroleraEnDisco() throws IOException {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaDocumentoDescargable();
        mockPlantillaObligatoria(TipoPlantilla.ALTA_SOCIO);

        SolicitudTarjetaDTO resultado = service.create(crearDTO(TipoSolicitud.ALTA, null));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(resultado.getNumeroSolicitud()).isEqualTo("TAR-" + Year.now().getValue() + "-00001");
        // La plantilla queda guardada dos veces: el original intacto y la copia editable.
        Path directorio = storageTarjetas.resolve(resultado.getNumeroSolicitud());
        assertThat(directorio.resolve("plantilla_original.pdf")).exists();
        assertThat(directorio.resolve("editable.pdf")).exists();
        assertThat(resultado.getRutaPdfEditable()).isEqualTo(directorio.resolve("editable.pdf").toString());
    }

    @Test
    void crearAltaSinPlantillaConfiguradaNoLlegaACrearLaSolicitud() throws IOException {
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), eq(TipoSolicitud.ALTA)))
                .thenThrow(new IOException("PLANTILLA_NO_CONFIGURADA: La petrolera no tiene configurada una plantilla"));

        assertThatThrownBy(() -> service.create(crearDTO(TipoSolicitud.ALTA, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("no tiene configurada una plantilla");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    /** Una LLEGADA no tiene papeleo: no descarga impreso ni pasa por el circuito de firma. */
    @Test
    void crearLlegadaNoEntraEnElCircuitoDeFirma() throws IOException {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_MADRID);

        SolicitudTarjetaDTO resultado = service.create(crearDTO(TipoSolicitud.LLEGADA, LocalDate.of(2026, 4, 10)));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        assertThat(resultado.getRutaPdfEditable()).isNull();
        verify(petrolerasClient, never()).obtenerPlantillaDocumento(any(), any());
    }

    @Test
    void enviarASocioExigeQueLaSolicitudSigaEnBorrador() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.ENVIADO_SOCIO);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("en borrador");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    /** Sin impreso en disco no se puede pedir una firma: ni cambia el estado ni sale correo. */
    @Test
    void enviarASocioSinImpresoNiCambiaElEstadoNiEnviaCorreo() {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.BORRADOR);
        solicitud.setRutaPdfEditable(storageTarjetas.resolve(NUMERO_SOLICITUD).resolve("editable.pdf").toString());
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("falta el impreso");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        verify(repository, never()).save(any(SolicitudTarjeta.class));
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    @Test
    void enviarASocioAplanaElImpresoLoAdjuntaYPasaAEnviadoSocio() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.BORRADOR);
        solicitud.setRutaPdfEditable(pdfEnDisco("editable.pdf"));
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_SOCIO);

        SolicitudTarjetaDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(resultado.getFechaEnvioSocio()).isNotNull();
        // Lo que viaja adjunto es el aplanado, no el editable.
        Path adjunto = capturarAdjunto(TipoPlantilla.DOCUMENTO_SOCIO);
        assertThat(adjunto).exists().hasFileName("enviado.pdf");
        assertThat(resultado.getCorreosEnviados()).contains(TipoPlantilla.DOCUMENTO_SOCIO.name());
    }

    @Test
    void enviarASocioSinPlantillaDeCorreoNoRompeLaTransicion() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.BORRADOR);
        solicitud.setRutaPdfEditable(pdfEnDisco("editable.pdf"));
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.DOCUMENTO_SOCIO)).thenReturn(Optional.empty());

        SolicitudTarjetaDTO resultado = service.enviarASocio(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(resultado.getCorreosEnviados()).isNull();
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    /** El escaneado puede venir mal: se puede reemplazar, así que el estado no avanza solo. */
    @Test
    void subirPdfFirmadoGuardaElDocumentoPeroNoCambiaElEstadoNiEnviaCorreo() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.ENVIADO_SOCIO);
        mockSolicitudGuardada(solicitud);

        SolicitudTarjetaDTO resultado = service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("escaneo.pdf"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(resultado.getFechaRecepcionFirmado()).isNotNull();
        assertThat(resultado.getNombrePdfFirmado()).isEqualTo("escaneo.pdf");
        assertThat(storageTarjetas.resolve(NUMERO_SOLICITUD).resolve("firmado.pdf")).exists();
        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    @Test
    void subirPdfFirmadoExigeQueElImpresoSeHayaEnviadoAlSocio() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.BORRADOR);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
        MockMultipartFile escaneo = multipartPdf("escaneo.pdf");

        assertThatThrownBy(() -> service.subirPdfFirmado(SOLICITUD_ID, escaneo))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("enviada al socio");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void aceptarFirmaSocioExigeTenerElImpresoFirmado() {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.ENVIADO_SOCIO);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.aceptarFirmaSocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("impreso firmado");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        verify(repository, never()).save(any(SolicitudTarjeta.class));
    }

    @Test
    void aceptarFirmaSocioPasaAFirmadoSocio() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.ENVIADO_SOCIO);
        solicitud.setRutaPdfFirmado(pdfEnDisco("firmado.pdf"));
        mockSolicitudGuardada(solicitud);

        assertThat(service.aceptarFirmaSocio(SOLICITUD_ID).getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
    }

    @Test
    void enviarAPetroleraExigeQueLaFirmaEsteAceptada() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.ENVIADO_SOCIO);
        solicitud.setRutaPdfFirmado(pdfEnDisco("firmado.pdf"));
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("firma del socio aceptada");

        verify(repository, never()).save(any(SolicitudTarjeta.class));
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    /** Sin el impreso firmado no hay nada que presentar: la solicitud no llega a PENDIENTE. */
    @Test
    void enviarAPetroleraSinImpresoFirmadoNiCambiaElEstadoNiEnviaCorreo() {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.FIRMADO_SOCIO);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarAPetrolera(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("falta el impreso firmado");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);
        verify(repository, never()).save(any(SolicitudTarjeta.class));
        verify(emailService, never()).enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                anyString(), any(Path.class), anyString());
    }

    @Test
    void enviarAPetroleraAdjuntaElImpresoFirmadoYPasaAPendiente() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.FIRMADO_SOCIO);
        solicitud.setRutaPdfFirmado(pdfEnDisco("firmado.pdf"));
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_PETROLERA);

        SolicitudTarjetaDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.getFechaEnvioPetrolera()).isNotNull();
        // Un correo por solicitud, a la petrolera, con la copia final adjunta.
        verify(emailService, times(1)).enviarCorreoConPlantillaYAdjunto(eq(petrolera.getEmail()), anyString(),
                anyString(), any(), eq(TipoPlantilla.DOCUMENTO_PETROLERA.name()), any(Path.class), anyString());
        assertThat(capturarAdjunto(TipoPlantilla.DOCUMENTO_PETROLERA)).exists().hasFileName("final.pdf");
    }

    /** Este correo es el que presenta la solicitud: sin plantilla sale igual, con texto propio. */
    @Test
    void enviarAPetroleraSinPlantillaUsaElTextoPorDefectoYEnviaIgual() throws IOException {
        SolicitudTarjeta solicitud = solicitudEnCircuito(TipoSolicitud.ALTA, EstadoSolicitud.FIRMADO_SOCIO);
        solicitud.setRutaPdfFirmado(pdfEnDisco("firmado.pdf"));
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.DOCUMENTO_PETROLERA)).thenReturn(Optional.empty());
        when(emailService.enviarCorreoConPlantillaYAdjunto(anyString(), anyString(), anyString(), any(),
                eq(TipoPlantilla.DOCUMENTO_PETROLERA.name()), any(Path.class), anyString()))
                .thenReturn(new EnvioCorreoResult(true, TipoPlantilla.DOCUMENTO_PETROLERA.name(), petrolera.getEmail()));

        SolicitudTarjetaDTO resultado = service.enviarAPetrolera(SOLICITUD_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCorreoConPlantillaYAdjunto(eq(petrolera.getEmail()), anyString(),
                cuerpo.capture(), any(), eq(TipoPlantilla.DOCUMENTO_PETROLERA.name()), any(Path.class), anyString());
        // El cuerpo por defecto lleva los datos del socio que la petrolera necesita.
        assertThat(cuerpo.getValue()).contains(NUMERO_SOLICITUD).contains("{nombre}").contains("{direccionCompleta}");
    }

    /** Las solicitudes anteriores al circuito no tienen número, y sin número no hay directorio. */
    @Test
    void unaSolicitudSinNumeroNoPuedeEntrarEnElCircuito() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        solicitud.setEstado(EstadoSolicitud.BORRADOR);
        solicitud.setNumeroSolicitud(null);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.enviarASocio(SOLICITUD_ID))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("no tiene número asignado");
    }

    @Test
    void denegarPorPetroleraGuardaElMotivoEnSuPropioCampo() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_RECHAZADA);

        SolicitudTarjetaDTO resultado = service.denegarPorPetrolera(SOLICITUD_ID, "Contrato no vigente");

        assertThat(resultado.getMotivoRechazo()).isEqualTo("Contrato no vigente");
    }

    // ---------- recorrido completo del ALTA ----------

    @Test
    void unAltaRecorreTodoSuCaminoHastaCompletada() throws IOException {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaDocumentoDescargable();
        mockPlantillaObligatoria(TipoPlantilla.ALTA_SOCIO);
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_MADRID);
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_SOCIO);
        mockPlantillaConAdjunto(TipoPlantilla.DOCUMENTO_PETROLERA);
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.ALTA_APROBADA)).thenReturn(Optional.empty());

        // 1. El alta nace con el impreso de la petrolera descargado y todavía editable.
        SolicitudTarjetaDTO creada = service.create(crearDTO(TipoSolicitud.ALTA, null));
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(creada.getNumeroSolicitud()).startsWith("TAR-" + Year.now().getValue() + "-");
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.ALTA_SOCIO.name()));

        // La solicitud viva es la que se guardo: se reutiliza en los pasos siguientes.
        ArgumentCaptor<SolicitudTarjeta> captor = ArgumentCaptor.forClass(SolicitudTarjeta.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        SolicitudTarjeta enCurso = captor.getValue();
        enCurso.setId(SOLICITUD_ID);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(enCurso));

        // 2. Se manda al socio para que lo firme.
        assertThat(service.enviarASocio(SOLICITUD_ID).getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);

        // 3. El socio lo devuelve firmado y la oficina da la firma por buena.
        service.subirPdfFirmado(SOLICITUD_ID, multipartPdf("firmado-escaneado.pdf"));
        assertThat(service.aceptarFirmaSocio(SOLICITUD_ID).getEstado()).isEqualTo(EstadoSolicitud.FIRMADO_SOCIO);

        // 4. Solo entonces se presenta a la petrolera, con el impreso firmado adjunto.
        assertThat(service.enviarAPetrolera(SOLICITUD_ID).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(capturarAdjunto(TipoPlantilla.DOCUMENTO_PETROLERA)).exists();

        // 5. La petrolera responde que si.
        assertThat(service.aprobarPorPetrolera(SOLICITUD_ID).getEstado()).isEqualTo(EstadoSolicitud.APROBADA);

        // 3. Llega la tarjeta fisica y se avisa al socio.
        SolicitudTarjetaDTO conLlegada = service.registrarLlegada(SOLICITUD_ID,
                new RegistrarLlegadaDTO(LocalDate.of(2026, 6, 1), "CTR-9876", null));
        assertThat(conLlegada.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);

        // 4. Se entrega al socio: se crea la tarjeta y el proceso queda cerrado.
        SolicitudTarjetaDTO entregada = service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));
        assertThat(entregada.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        assertThat(entregada.getFechaEntrega()).isNotNull();
        verify(tarjetaService, times(1)).create(any(Tarjeta.class));
    }
}
