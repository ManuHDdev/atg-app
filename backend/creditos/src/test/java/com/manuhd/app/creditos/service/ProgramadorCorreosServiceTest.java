package com.manuhd.app.creditos.service;

import com.manuhd.app.creditos.model.Credito;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.repository.CreditoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de los envíos automáticos de créditos.
 * Sin contexto de Spring ni base de datos: solo JUnit 5 + Mockito.
 *
 * La fecha "de hoy" se inyecta a través de las sobrecargas package-private de los
 * jobs, para que los tests no dependan del día real en el que se ejecutan.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProgramadorCorreosService - envíos automáticos")
class ProgramadorCorreosServiceTest {

    /** 5 de enero de 2026 es lunes. */
    private static final LocalDate LUNES = LocalDate.of(2026, 1, 5);
    private static final Long PETROLERA_ID = 20L;
    private static final String PETROLERAS_URL = "http://petroleras-test";

    @Mock
    private CreditoRepository creditoRepository;

    @Mock
    private CreditoService creditoService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProgramadorCorreosService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "petrolerasBaseUrl", PETROLERAS_URL);
    }

    private Credito credito(Long id, Boolean programado, LocalDate fechaProgramada) {
        Credito credito = new Credito();
        credito.setId(id);
        credito.setSocioId(1L);
        credito.setPetroleraId(PETROLERA_ID);
        credito.setEstado(EstadoCredito.PENDIENTE);
        credito.setProgramadoEnvio(programado);
        credito.setFechaProgramadaEnvio(fechaProgramada);
        return credito;
    }

    private void petroleraEnviaLunes() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", PETROLERA_ID, "diasEnvioCreditos", "LUNES,JUEVES"));
    }

    @Test
    @DisplayName("El job por día de semana no envía un crédito con fecha programada propia, aunque hoy toque envío")
    void noEnviaCreditoConProgramacionPropia() {
        Credito programado = credito(1L, true, LUNES.plusDays(15));
        // El repositorio ya filtra estos créditos; aquí se devuelve a propósito para
        // comprobar la barrera del servicio, que es la que fija la regla de negocio.
        when(creditoRepository.findByEstadoSinProgramacionPropia(EstadoCredito.PENDIENTE))
                .thenReturn(List.of(programado));

        service.procesarCreditosPorDiaSemana(LUNES);

        verify(creditoService, never()).enviarAPetrolera(programado.getId());
        verify(restTemplate, never()).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("El job por día de semana envía los créditos sin programación propia cuando hoy toca envío")
    void enviaCreditoSinProgramacionPropia() {
        Credito sinProgramar = credito(2L, false, null);
        when(creditoRepository.findByEstadoSinProgramacionPropia(EstadoCredito.PENDIENTE))
                .thenReturn(List.of(sinProgramar));
        petroleraEnviaLunes();

        service.procesarCreditosPorDiaSemana(LUNES);

        verify(creditoService).enviarAPetrolera(sinProgramar.getId());
    }

    @Test
    @DisplayName("El job por día de semana no envía nada si hoy no es día de envío de la petrolera")
    void noEnviaSiHoyNoEsDiaDeEnvio() {
        Credito sinProgramar = credito(3L, false, null);
        when(creditoRepository.findByEstadoSinProgramacionPropia(EstadoCredito.PENDIENTE))
                .thenReturn(List.of(sinProgramar));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", PETROLERA_ID, "diasEnvioCreditos", "JUEVES"));

        service.procesarCreditosPorDiaSemana(LUNES);

        verify(creditoService, never()).enviarAPetrolera(sinProgramar.getId());
    }

    @Test
    @DisplayName("El job por día de semana nunca consulta todos los pendientes sin filtrar")
    void nuncaUsaElBuscadorSinFiltrar() {
        when(creditoRepository.findByEstadoSinProgramacionPropia(EstadoCredito.PENDIENTE))
                .thenReturn(List.of());

        service.procesarCreditosPorDiaSemana(LUNES);

        verify(creditoRepository, never()).findByEstado(EstadoCredito.PENDIENTE);
    }

    @Test
    @DisplayName("El job diario envía los créditos cuya fecha programada es hoy")
    void envioDiarioProcesaLosProgramadosParaHoy() {
        Credito programadoHoy = credito(4L, true, LUNES);
        when(creditoRepository.findByProgramadoEnvioTrueAndFechaProgramadaEnvio(LUNES))
                .thenReturn(List.of(programadoHoy));

        service.procesarCreditosProgramados(LUNES);

        verify(creditoService).enviarAPetrolera(programadoHoy.getId());
    }

    @Test
    @DisplayName("El job diario no envía nada si ningún crédito está programado para hoy")
    void envioDiarioNoHaceNadaSinCreditosParaHoy() {
        when(creditoRepository.findByProgramadoEnvioTrueAndFechaProgramadaEnvio(LUNES))
                .thenReturn(List.of());

        service.procesarCreditosProgramados(LUNES);

        verify(creditoService, never()).enviarAPetrolera(org.mockito.ArgumentMatchers.anyLong());
    }
}
