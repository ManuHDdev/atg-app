package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.dto.AprobarBajaDTO;
import com.manuhd.app.tarjetas.dto.AprobarDuplicadoDTO;
import com.manuhd.app.tarjetas.dto.EnvioCorreoResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private SolicitudTarjetaService service;

    private SocioDTO socio;
    private PetroleraDTO petrolera;

    @BeforeEach
    void setUp() {
        socio = new SocioDTO(SOCIO_ID, "Transportes Ejemplo SL", EMAIL_SOCIO, "600111222",
                "Calle Mayor 1", "Alcalá de Henares", "28801", "Madrid", "S-001");
        petrolera = new PetroleraDTO(PETROLERA_ID, "Repsol", "petrolera@example.com");
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

    // ---------- aprobar ----------

    @Test
    void aprobarEnviaCorreoDeAltaAprobadaYPasaAAprobada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_APROBADA);

        SolicitudTarjetaDTO resultado = service.aprobar(SOLICITUD_ID, "oficina");

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.ALTA_APROBADA.name()));
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.ALTA_APROBADA.name());
    }

    @Test
    void aprobarSinPlantillaActivaNoRompeLaTransicionNiRegistraCorreos() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(plantillaService.buscarPlantillaActiva(TipoPlantilla.ALTA_APROBADA)).thenReturn(Optional.empty());

        SolicitudTarjetaDTO resultado = service.aprobar(SOLICITUD_ID, "oficina");

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(solicitud.getCorreosEnviados()).isNull();
        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), any(), anyString());
    }

    // ---------- rechazar ----------

    @Test
    void rechazarEnviaCorreoDeAltaRechazadaConElMotivoYPasaARechazada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.ALTA);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        mockPlantillaActiva(TipoPlantilla.ALTA_RECHAZADA);

        SolicitudTarjetaDTO resultado = service.rechazar(SOLICITUD_ID, "Contrato no vigente", "oficina");

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(capturarVariables(TipoPlantilla.ALTA_RECHAZADA))
                .containsEntry("motivo", "Contrato no vigente");
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.ALTA_RECHAZADA.name());
    }

    // ---------- aprobarBaja / aprobarDuplicado ----------

    @Test
    void aprobarBajaEnviaBajaConfirmadaYNoBajaSocio() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.BAJA);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        mockPlantillaActiva(TipoPlantilla.BAJA_CONFIRMADA);

        AprobarBajaDTO dto = new AprobarBajaDTO(LocalDate.of(2026, 1, 15), "oficina", null);
        SolicitudTarjetaDTO resultado = service.aprobarBaja(SOLICITUD_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        verify(emailService).enviarCorreoConPlantilla(eq(EMAIL_SOCIO), anyString(), anyString(), any(),
                eq(TipoPlantilla.BAJA_CONFIRMADA.name()));
        verify(plantillaService, never()).buscarPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
        verify(plantillaService, never()).obtenerPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
        assertThat(solicitud.getCorreosEnviados()).contains(TipoPlantilla.BAJA_CONFIRMADA.name());
    }

    @Test
    void aprobarDuplicadoEnviaDuplicadoConfirmadaYPasaACompletada() {
        SolicitudTarjeta solicitud = solicitud(TipoSolicitud.DUPLICADO);
        solicitud.setTarjetaId(TARJETA_ID);
        mockSolicitudGuardada(solicitud);
        mockServiciosExternos();
        when(tarjetaService.findById(TARJETA_ID)).thenReturn(tarjeta(1));
        mockPlantillaActiva(TipoPlantilla.DUPLICADO_CONFIRMADA);

        AprobarDuplicadoDTO dto = new AprobarDuplicadoDTO(LocalDate.of(2026, 2, 1), "oficina", null);
        SolicitudTarjetaDTO resultado = service.aprobarDuplicado(SOLICITUD_ID, dto);

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

        RegistrarLlegadaDTO dto = new RegistrarLlegadaDTO(LocalDate.of(2026, 3, 1), "CTR-9876", null, "oficina");
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

        service.aprobar(SOLICITUD_ID, "oficina");

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

        service.aprobar(SOLICITUD_ID, "oficina");

        assertThat(capturarVariables(TipoPlantilla.ALTA_APROBADA)).containsEntry("numeroContrato", "");
    }
}
