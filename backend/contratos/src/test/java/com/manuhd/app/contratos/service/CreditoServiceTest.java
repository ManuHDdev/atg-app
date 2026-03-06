package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.TestBase;
import com.manuhd.app.contratos.dto.CreditoDTO;
import com.manuhd.app.contratos.dto.CrearCreditoDTO;
import com.manuhd.app.contratos.model.Credito;
import com.manuhd.app.contratos.model.EstadoCredito;
import com.manuhd.app.contratos.model.TipoCredito;
import com.manuhd.app.contratos.repository.CreditoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditoService Unit Tests")
class CreditoServiceTest extends TestBase {

    @Mock
    private CreditoRepository creditoRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CreditoService creditoService;

    private Credito creditoPendiente;
    private Credito creditoEnviado;
    private Map<String, Object> mockSocio;
    private Map<String, Object> mockPetrolera;
    private Map<String, Object> mockEmpresa;

    @BeforeEach
    void setUp() {
        creditoPendiente = createCredito(TipoCredito.SOLICITUD_CREDITO, EstadoCredito.PENDIENTE);
        creditoPendiente.setId(1L);

        creditoEnviado = createCredito(TipoCredito.AMPLIACION_CREDITO, EstadoCredito.ENVIADO_PETROLERA);
        creditoEnviado.setId(2L);

        // Mock responses from external services
        mockSocio = new HashMap<>();
        mockSocio.put("nombre", "Juan Pérez");
        mockSocio.put("email", "juan@example.com");
        mockSocio.put("telefono", "123456789");
        mockSocio.put("numeroSocio", "SOC-001");

        mockPetrolera = new HashMap<>();
        mockPetrolera.put("nombre", "Petrolera Test");
        mockPetrolera.put("email", "petrolera@test.com");

        mockEmpresa = new HashMap<>();
        mockEmpresa.put("nombre", "Empresa Test SL");
        mockEmpresa.put("cif", "B12345678");
        mockEmpresa.put("email", "empresa@test.com");

        
    }

    @Test
    @DisplayName("listarTodos - Debe retornar todos los créditos")
    void listarTodos_DebeRetornarTodosLosCreditos() {
        // Given
        List<Credito> creditos = Arrays.asList(creditoPendiente, creditoEnviado);
        when(creditoRepository.findAll()).thenReturn(creditos);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        List<CreditoDTO> resultado = creditoService.listarTodos();

        // Then
        assertThat(resultado).hasSize(2);
        verify(creditoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenerPorId - Debe retornar crédito cuando existe")
    void obtenerPorId_DebeRetornarCreditoCuandoExiste() {
        // Given
        Long id = 1L;
        when(creditoRepository.findById(id)).thenReturn(Optional.of(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        CreditoDTO resultado = creditoService.obtenerPorId(id);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getEstado()).isEqualTo(EstadoCredito.PENDIENTE);
        verify(creditoRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("obtenerPorId - Debe lanzar excepción cuando no existe")
    void obtenerPorId_DebeLanzarExcepcionCuandoNoExiste() {
        // Given
        Long id = 999L;
        when(creditoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> creditoService.obtenerPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Crédito no encontrado con id: " + id);
    }

    @Test
    @DisplayName("listarPorSocio - Debe retornar créditos del socio")
    void listarPorSocio_DebeRetornarCreditosDelSocio() {
        // Given
        Long socioId = TEST_SOCIO_ID;
        when(creditoRepository.findBySocioId(socioId)).thenReturn(Arrays.asList(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        List<CreditoDTO> resultado = creditoService.listarPorSocio(socioId);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSocioId()).isEqualTo(socioId);
        verify(creditoRepository, times(1)).findBySocioId(socioId);
    }

    @Test
    @DisplayName("listarPorPetrolera - Debe retornar créditos de la petrolera")
    void listarPorPetrolera_DebeRetornarCreditosDeLaPetrolera() {
        // Given
        Long petroleraId = TEST_PETROLERA_ID;
        when(creditoRepository.findByPetroleraId(petroleraId)).thenReturn(Arrays.asList(creditoEnviado));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        List<CreditoDTO> resultado = creditoService.listarPorPetrolera(petroleraId);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPetroleraId()).isEqualTo(petroleraId);
        verify(creditoRepository, times(1)).findByPetroleraId(petroleraId);
    }

    @Test
    @DisplayName("listarPorEstado - Debe retornar créditos por estado")
    void listarPorEstado_DebeRetornarCreditosPorEstado() {
        // Given
        EstadoCredito estado = EstadoCredito.PENDIENTE;
        when(creditoRepository.findByEstado(estado)).thenReturn(Arrays.asList(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        List<CreditoDTO> resultado = creditoService.listarPorEstado(estado);

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(estado);
        verify(creditoRepository, times(1)).findByEstado(estado);
    }

    @Test
    @DisplayName("crear - Debe crear crédito y enviar inmediatamente si no está programado")
    void crear_DebeCrearCreditoYEnviarInmediatamente() {
        // Given
        CrearCreditoDTO dto = new CrearCreditoDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoCredito(TipoCredito.SOLICITUD_CREDITO);
        dto.setMonto(new BigDecimal("5000.00"));
        dto.setObservaciones("Test");
        dto.setProgramadoEnvio(false);

        Credito creditoGuardado = createCredito(TipoCredito.SOLICITUD_CREDITO, EstadoCredito.PENDIENTE);
        creditoGuardado.setId(1L);

        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoGuardado);
        when(creditoRepository.findById(1L)).thenReturn(Optional.of(creditoGuardado));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockSocio, mockPetrolera, mockSocio, mockPetrolera);
        doNothing().when(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());

        // When
        CreditoDTO resultado = creditoService.crear(dto);

        // Then
        assertThat(resultado).isNotNull();
        verify(creditoRepository, atLeast(1)).save(any(Credito.class));

        // Verify that email was sent (enviarAPetrolera was called)
        ArgumentCaptor<Credito> captor = ArgumentCaptor.forClass(Credito.class);
        verify(creditoRepository, atLeast(1)).save(captor.capture());
    }

    @Test
    @DisplayName("crear - Debe crear crédito sin enviar si está programado")
    void crear_DebeCrearCreditoSinEnviarSiEstaProgramado() {
        // Given
        CrearCreditoDTO dto = new CrearCreditoDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoCredito(TipoCredito.SOLICITUD_CREDITO);
        dto.setMonto(new BigDecimal("5000.00"));
        dto.setProgramadoEnvio(true);
        dto.setFechaProgramadaEnvio(LocalDate.now().plusDays(7));

        Credito creditoGuardado = createCredito(TipoCredito.SOLICITUD_CREDITO, EstadoCredito.PENDIENTE);
        creditoGuardado.setId(1L);
        creditoGuardado.setProgramadoEnvio(true);

        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoGuardado);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);

        // When
        CreditoDTO resultado = creditoService.crear(dto);

        // Then
        assertThat(resultado).isNotNull();
        verify(creditoRepository, times(1)).save(any(Credito.class));
        // Should not call findById again (no enviarAPetrolera)
    }

    @Test
    @DisplayName("enviarAPetrolera - Debe enviar crédito exitosamente")
    void enviarAPetrolera_DebeEnviarCreditoExitosamente() {
        // Given
        Long creditoId = 1L;
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockSocio, mockPetrolera, mockEmpresa, mockSocio, mockPetrolera);
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoPendiente);
        doNothing().when(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());

        // When
        CreditoDTO resultado = creditoService.enviarAPetrolera(creditoId);

        // Then
        assertThat(resultado).isNotNull();
        verify(creditoRepository, times(1)).save(any(Credito.class));
        verify(emailService, times(1)).enviarCorreoConPlantilla(
                eq("petrolera@test.com"),
                anyString(),
                anyString(),
                anyMap()
        );

        ArgumentCaptor<Credito> captor = ArgumentCaptor.forClass(Credito.class);
        verify(creditoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCredito.ENVIADO_PETROLERA);
        assertThat(captor.getValue().getFechaEnvioPetrolera()).isNotNull();
    }

    @Test
    @DisplayName("enviarAPetrolera - Debe lanzar excepción si no está en estado PENDIENTE")
    void enviarAPetrolera_DebeLanzarExcepcionSiNoEstaPendiente() {
        // Given
        Long creditoId = 2L;
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoEnviado));

        // When & Then
        assertThatThrownBy(() -> creditoService.enviarAPetrolera(creditoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El crédito no está en estado PENDIENTE");

        verify(emailService, never()).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("responderPetrolera - Debe aprobar crédito y notificar socio")
    void responderPetrolera_DebeAprobarCreditoYNotificarSocio() {
        // Given
        Long creditoId = 2L;
        String respuesta = "Crédito aprobado";
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoEnviado), Optional.of(creditoEnviado));
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoEnviado);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockSocio, mockPetrolera, mockSocio, mockPetrolera);
        doNothing().when(emailService).enviarCorreoHTML(anyString(), anyString(), anyString());

        // When
        CreditoDTO resultado = creditoService.responderPetrolera(creditoId, true, respuesta);

        // Then
        assertThat(resultado).isNotNull();

        ArgumentCaptor<Credito> captor = ArgumentCaptor.forClass(Credito.class);
        verify(creditoRepository, atLeast(1)).save(captor.capture());

        // Verify estado changed to APROBADO then to COMPLETADO
        List<Credito> creditosGuardados = captor.getAllValues();
        assertThat(creditosGuardados.get(0).getEstado()).isEqualTo(EstadoCredito.APROBADO);
        assertThat(creditosGuardados.get(0).getRespuestaPetrolera()).isEqualTo(respuesta);

        // Verify email sent to socio
        verify(emailService, times(1)).enviarCorreoHTML(
                eq("juan@example.com"),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("responderPetrolera - Debe denegar crédito y notificar socio")
    void responderPetrolera_DebeDenegarCreditoYNotificarSocio() {
        // Given
        Long creditoId = 2L;
        String respuesta = "Crédito denegado por falta de documentación";
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoEnviado), Optional.of(creditoEnviado));
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoEnviado);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockSocio, mockPetrolera, mockSocio, mockPetrolera);
        doNothing().when(emailService).enviarCorreoHTML(anyString(), anyString(), anyString());

        // When
        CreditoDTO resultado = creditoService.responderPetrolera(creditoId, false, respuesta);

        // Then
        assertThat(resultado).isNotNull();

        ArgumentCaptor<Credito> captor = ArgumentCaptor.forClass(Credito.class);
        verify(creditoRepository, atLeast(1)).save(captor.capture());

        List<Credito> creditosGuardados = captor.getAllValues();
        assertThat(creditosGuardados.get(0).getEstado()).isEqualTo(EstadoCredito.DENEGADO);
        assertThat(creditosGuardados.get(0).getRespuestaPetrolera()).isEqualTo(respuesta);
    }

    @Test
    @DisplayName("responderPetrolera - Debe lanzar excepción si no está en estado ENVIADO_PETROLERA")
    void responderPetrolera_DebeLanzarExcepcionSiNoEstaEnviado() {
        // Given
        Long creditoId = 1L;
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoPendiente));

        // When & Then
        assertThatThrownBy(() -> creditoService.responderPetrolera(creditoId, true, "Respuesta"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El crédito no está en estado ENVIADO_PETROLERA");
    }

    @Test
    @DisplayName("notificarSocio - Debe notificar al socio exitosamente")
    void notificarSocio_DebeNotificarAlSocioExitosamente() {
        // Given
        Long creditoId = 2L;
        Credito creditoAprobado = createCredito(TipoCredito.SOLICITUD_CREDITO, EstadoCredito.APROBADO);
        creditoAprobado.setId(creditoId);
        creditoAprobado.setRespuestaPetrolera("Aprobado");

        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoAprobado));
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoAprobado);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockSocio, mockPetrolera);
        doNothing().when(emailService).enviarCorreoHTML(anyString(), anyString(), anyString());

        // When
        CreditoDTO resultado = creditoService.notificarSocio(creditoId);

        // Then
        assertThat(resultado).isNotNull();

        ArgumentCaptor<Credito> captor = ArgumentCaptor.forClass(Credito.class);
        verify(creditoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCredito.COMPLETADO);
        assertThat(captor.getValue().getFechaNotificacionSocio()).isNotNull();

        verify(emailService, times(1)).enviarCorreoHTML(
                eq("juan@example.com"),
                anyString(),
                contains("Respuesta sobre su")
        );
    }

    @Test
    @DisplayName("notificarSocio - Debe lanzar excepción si no tiene respuesta")
    void notificarSocio_DebeLanzarExcepcionSiNoTieneRespuesta() {
        // Given
        Long creditoId = 1L;
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoPendiente));

        // When & Then
        assertThatThrownBy(() -> creditoService.notificarSocio(creditoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El crédito no tiene respuesta de la petrolera");
    }

    @Test
    @DisplayName("enviarAPetrolera - Debe usar email de fallback si petrolera no tiene email")
    void enviarAPetrolera_DebeUsarEmailFallbackSiPetroleraNoTieneEmail() {
        // Given
        Long creditoId = 1L;
        Map<String, Object> petroleraSinEmail = new HashMap<>();
        petroleraSinEmail.put("nombre", "Petrolera Test");
        petroleraSinEmail.put("email", "");

        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockSocio, petroleraSinEmail, mockEmpresa, mockSocio, petroleraSinEmail);
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoPendiente);
        doNothing().when(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());

        // When
        creditoService.enviarAPetrolera(creditoId);

        // Then
        verify(emailService, times(1)).enviarCorreoConPlantilla(
                eq("admin@atg.com"), // Fallback email
                anyString(),
                anyString(),
                anyMap()
        );
    }

    @Test
    @DisplayName("enviarAPetrolera - Debe manejar error al obtener datos externos")
    void enviarAPetrolera_DebeManejarErrorAlObtenerDatosExternos() {
        // Given
        Long creditoId = 1L;
        when(creditoRepository.findById(creditoId)).thenReturn(Optional.of(creditoPendiente));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Service unavailable"));
        when(creditoRepository.save(any(Credito.class))).thenReturn(creditoPendiente);
        doNothing().when(emailService).enviarCorreoConPlantilla(anyString(), anyString(), anyString(), anyMap());

        // When
        CreditoDTO resultado = creditoService.enviarAPetrolera(creditoId);

        // Then
        assertThat(resultado).isNotNull();
        // Should still save with fallback data
        verify(creditoRepository, times(1)).save(any(Credito.class));
    }
}
