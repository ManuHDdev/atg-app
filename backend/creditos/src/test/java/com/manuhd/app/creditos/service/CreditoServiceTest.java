package com.manuhd.app.creditos.service;

import com.manuhd.app.creditos.dto.CrearCreditoDTO;
import com.manuhd.app.creditos.dto.CreditoDTO;
import com.manuhd.app.creditos.dto.PetroleraDTO;
import com.manuhd.app.creditos.model.Credito;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.model.TipoCredito;
import com.manuhd.app.creditos.repository.CreditoRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la restricción de petrolera al crear créditos.
 *
 * El procedimiento de ATG limita los créditos a determinadas petroleras. La restricción es
 * configurable por petrolera y un flag sin valor (null) significa "sin restricción
 * configurada" => permitido, para no bloquear a las petroleras ya existentes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditoServiceTest {

    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final String PETROLERAS_URL = "http://petroleras:8082";
    private static final String URL_PETROLERA = PETROLERAS_URL + "/api/petroleras/" + PETROLERA_ID;

    @Mock
    private CreditoRepository creditoRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CreditoService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "petrolerasBaseUrl", PETROLERAS_URL);
        ReflectionTestUtils.setField(service, "sociosBaseUrl", "http://socios:8081");

        when(creditoRepository.save(any(Credito.class))).thenAnswer(invocation -> {
            Credito guardado = invocation.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });
    }

    private PetroleraDTO petrolera(Boolean operaCreditos) {
        PetroleraDTO dto = new PetroleraDTO();
        dto.setId(PETROLERA_ID);
        dto.setNombre("Solred");
        dto.setActiva(true);
        dto.setOperaCreditos(operaCreditos);
        return dto;
    }

    private CrearCreditoDTO nuevoCredito() {
        CrearCreditoDTO dto = new CrearCreditoDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setTipoCredito(TipoCredito.SOLICITUD_CREDITO);
        dto.setMonto(new BigDecimal("2500.00"));
        // Programado: evita el envío inmediato de correo a la petrolera
        dto.setProgramadoEnvio(true);
        return dto;
    }

    @Test
    void rechazaElCreditoSiLaPetroleraNoOperaConCreditos() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(false));

        assertThatThrownBy(() -> service.crear(nuevoCredito()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solred")
                .hasMessageContaining("no opera con créditos");

        verify(creditoRepository, never()).save(any(Credito.class));
    }

    @Test
    void permiteElCreditoCuandoElFlagEstaSinConfigurar() {
        // null = sin restricción configurada => se permite
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(null));

        CreditoDTO creado = service.crear(nuevoCredito());

        assertThat(creado.getEstado()).isEqualTo(EstadoCredito.PENDIENTE);
        assertThat(creado.getPetroleraId()).isEqualTo(PETROLERA_ID);
        verify(creditoRepository).save(any(Credito.class));
    }

    @Test
    void permiteElCreditoCuandoLaPetroleraOperaConCreditos() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenReturn(petrolera(true));

        CreditoDTO creado = service.crear(nuevoCredito());

        assertThat(creado.getEstado()).isEqualTo(EstadoCredito.PENDIENTE);
        verify(creditoRepository).save(any(Credito.class));
    }

    @Test
    void rechazaElCreditoSiNoSePuedeConsultarLaPetrolera() {
        when(restTemplate.getForObject(eq(URL_PETROLERA), eq(PetroleraDTO.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.crear(nuevoCredito()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se ha podido verificar la petrolera");

        verify(creditoRepository, never()).save(any(Credito.class));
    }

    // --- Importe concedido por la petrolera ---------------------------------------------

    private Credito creditoEnviado(TipoCredito tipo, BigDecimal monto) {
        Credito credito = new Credito();
        credito.setId(1L);
        credito.setSocioId(SOCIO_ID);
        credito.setPetroleraId(PETROLERA_ID);
        credito.setTipoCredito(tipo);
        credito.setEstado(EstadoCredito.ENVIADO_PETROLERA);
        credito.setMonto(monto);
        when(creditoRepository.findById(1L)).thenReturn(Optional.of(credito));

        // La notificacion al socio consulta el microservicio de socios
        java.util.Map<String, Object> socio = new java.util.HashMap<>();
        socio.put("nombre", "Transportes Perez");
        socio.put("email", "socio@example.com");
        when(restTemplate.getForObject(eq("http://socios:8081/api/socios/" + SOCIO_ID), eq(java.util.Map.class)))
                .thenReturn(socio);
        return credito;
    }

    @Test
    void registraElImporteConcedidoAlAprobar() {
        Credito credito = creditoEnviado(TipoCredito.SOLICITUD_CREDITO, new BigDecimal("12000.00"));

        service.responderPetrolera(1L, true, "OK", new BigDecimal("12000.00"));

        assertThat(credito.getMontoConcedido()).isEqualByComparingTo("12000.00");
        assertThat(credito.getMonto()).isEqualByComparingTo("12000.00");
    }

    @Test
    void persisteUnImporteConcedidoDistintoDelSolicitado() {
        Credito credito = creditoEnviado(TipoCredito.SOLICITUD_CREDITO, new BigDecimal("50000.00"));

        service.responderPetrolera(1L, true, "Concedido parcialmente", new BigDecimal("30000.00"));

        assertThat(credito.getMonto()).isEqualByComparingTo("50000.00");
        assertThat(credito.getMontoConcedido()).isEqualByComparingTo("30000.00");
    }

    @Test
    void rechazaLaAprobacionSinImporteConcedido() {
        Credito credito = creditoEnviado(TipoCredito.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("importe concedido");

        assertThat(credito.getEstado()).isEqualTo(EstadoCredito.ENVIADO_PETROLERA);
        verify(creditoRepository, never()).save(any(Credito.class));
    }

    @Test
    void rechazaLaAprobacionConImporteConcedidoCeroONegativo() {
        creditoEnviado(TipoCredito.SOLICITUD_CREDITO, new BigDecimal("2000.00"));

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor que 0");

        assertThatThrownBy(() -> service.responderPetrolera(1L, true, "OK", new BigDecimal("-10")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor que 0");

        verify(creditoRepository, never()).save(any(Credito.class));
    }

    @Test
    void alDenegarNoSeGuardaImporteConcedido() {
        Credito credito = creditoEnviado(TipoCredito.SOLICITUD_CREDITO, new BigDecimal("4000.00"));

        // Aunque llegue un importe, al denegar se ignora y queda a null
        service.responderPetrolera(1L, false, "Sin riesgo suficiente", new BigDecimal("4000.00"));

        assertThat(credito.getMontoConcedido()).isNull();
    }

    @Test
    void laDevolucionDeAvalSeApruebaSinImporteConcedido() {
        Credito credito = creditoEnviado(TipoCredito.DEVOLUCION_AVAL, null);

        service.responderPetrolera(1L, true, "OK", null);

        assertThat(credito.getMontoConcedido()).isNull();
        assertThat(credito.getEstado()).isEqualTo(EstadoCredito.COMPLETADO_APROBADO);
    }
}
