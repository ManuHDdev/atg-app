package com.manuhd.app.dispositivos.service;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @InjectMocks
    private SolicitudDispositivoService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "petrolerasBaseUrl", PETROLERAS_URL);
        ReflectionTestUtils.setField(service, "sociosBaseUrl", "http://socios:8081");

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
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
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
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
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
}
