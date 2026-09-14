package com.manuhd.app.tarjetas.service;

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
import com.manuhd.app.tarjetas.model.PlantillaTarjeta;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.Tarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import com.manuhd.app.tarjetas.security.UsuarioActualService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Colaborador real: los tests ejercitan la lectura del JWT, no un doble de prueba.
    @Spy
    private UsuarioActualService usuarioActual = new UsuarioActualService();

    @InjectMocks
    private SolicitudTarjetaService service;

    private SocioDTO socio;
    private PetroleraDTO petrolera;

    @BeforeEach
    void setUp() {
        socio = new SocioDTO(SOCIO_ID, "Transportes Ejemplo SL", EMAIL_SOCIO, "600111222",
                "Calle Mayor 1", "Alcalá de Henares", "28801", "Madrid", "S-001");
        petrolera = new PetroleraDTO(PETROLERA_ID, "Repsol", "petrolera@example.com");
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

    @Test
    void aprobarDuplicadoPorPetroleraEnviaDuplicadoConfirmadaYPasaACompletada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        mockPlantillaActiva(TipoPlantilla.DUPLICADO_CONFIRMADA);

        AprobarDuplicadoDTO dto = new AprobarDuplicadoDTO(LocalDate.of(2026, 2, 1), null);
        SolicitudTarjetaDTO resultado = service.aprobarDuplicadoPorPetrolera(SOLICITUD_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.DUPLICADO_CONFIRMADA.name()));
        verify(plantillaService, never()).obtenerPlantillaActiva(TipoPlantilla.DUPLICADO_SOCIO);
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.DUPLICADO_CONFIRMADA.name());
    }

    // ---------- regla Madrid ----------

    @ParameterizedTest
    @ValueSource(strings = {"Madrid", "  madrid  ", "MADRID ", "Comunidad de Madrid", "COMUNIDAD DE MADRÍD"})
    void registrarLlegadaUsaLaPlantillaDeMadridParaLaComunidadDeMadrid(String provincia) {
        assertThat(service.esProvinciaMadrid(provincia)).isTrue();
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_MADRID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Toledo", "", "   "})
    void registrarLlegadaUsaLaPlantillaPostalFueraDeMadrid(String provincia) {
        assertThat(service.esProvinciaMadrid(provincia)).isFalse();
        registrarLlegadaCon(provincia, TipoPlantilla.LLEGADA_FUERA);
    }

    private void registrarLlegadaCon(String provincia, TipoPlantilla esperada) {
        socio.setProvincia(provincia);

        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
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
    @EnumSource(value = TipoSolicitud.class, names = {"DUPLICADO", "BAJA", "LLEGADA"})
    void marcarEntregadaNoCreaTarjetaSiNoEsUnAlta(TipoSolicitud tipo) {
        SolicitudTarjeta solicitud = solicitud(tipo);
        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        mockSolicitudGuardada(solicitud);

        service.marcarEntregada(SOLICITUD_ID, new MarcarEntregadaDTO(null, null));

        verify(tarjetaService, never()).create(any(Tarjeta.class));
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
                .hasMessageContaining("solicitudes de ALTA");

        // Lo importante: no se reenvia el aviso de llegada al socio.
        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
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

    // ---------- recorrido completo del ALTA ----------

    @Test
    void unAltaRecorreTodoSuCaminoHastaCompletada() {
        mockGuardadoDeNuevaSolicitud();
        mockServiciosExternos();
        mockPlantillaObligatoria(TipoPlantilla.ALTA_SOCIO);
        mockPlantillaObligatoria(TipoPlantilla.ALTA_PETROLERA);
        mockPlantillaObligatoria(TipoPlantilla.LLEGADA_MADRID);
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.ALTA_APROBADA)).thenReturn(Optional.empty());

        // 1. Se presenta el alta a la petrolera: queda pendiente de su respuesta.
        SolicitudTarjetaDTO creada = service.create(crearDTO(TipoSolicitud.ALTA, null));
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.ALTA_SOCIO.name()));

        // La solicitud viva es la que se guardo: se reutiliza en los pasos siguientes.
        ArgumentCaptor<SolicitudTarjeta> captor = ArgumentCaptor.forClass(SolicitudTarjeta.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        SolicitudTarjeta enCurso = captor.getValue();
        enCurso.setId(SOLICITUD_ID);
        when(repository.findById(SOLICITUD_ID)).thenReturn(Optional.of(enCurso));

        // 2. La petrolera responde que si.
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
