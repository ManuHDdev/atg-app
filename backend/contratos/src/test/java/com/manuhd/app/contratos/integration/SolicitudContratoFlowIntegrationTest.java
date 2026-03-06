package com.manuhd.app.contratos.integration;

import com.manuhd.app.contratos.TestBase;
import com.manuhd.app.contratos.client.PetrolerasClient;
import com.manuhd.app.contratos.dto.CrearSolicitudDTO;
import com.manuhd.app.contratos.dto.SolicitudContratoDTO;
import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.model.TipoSolicitudContrato;
import com.manuhd.app.contratos.repository.SolicitudContratoRepository;
import com.manuhd.app.contratos.service.SolicitudContratoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete Solicitud Contrato workflow:
 * BORRADOR -> ENVIADO_SOCIO -> FIRMADO_SOCIO -> ENVIADO_PETROLERA
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SolicitudContrato Flow Integration Tests")
class SolicitudContratoFlowIntegrationTest extends TestBase {

    @Autowired
    private SolicitudContratoService solicitudContratoService;

    @Autowired
    private SolicitudContratoRepository solicitudContratoRepository;

    @MockitoBean
    private PetrolerasClient petrolerasClient;

    @Test
    @DisplayName("Flujo completo - Crear solicitud en estado BORRADOR")
    void flujocompleto_CrearSolicitudEnBorrador() throws Exception {
        // Given
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setEmpresaId(TEST_EMPRESA_ID);
        dto.setTarjetaId(TEST_TARJETA_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto.setTipoSolicitudPetroleraId(TEST_TIPO_SOLICITUD_PETROLERA_ID);
        dto.setSolicitadoPor("Test User");
        dto.setEsAutonomo(false);
        dto.setObservaciones("Test");
        dto.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        byte[] mockPdf = "%PDF-1.4\nTest PDF".getBytes();
        when(petrolerasClient.tienePlantillaPdf(anyLong())).thenReturn(true);
        when(petrolerasClient.obtenerPlantillaPdf(anyLong())).thenReturn(mockPdf);

        // When
        SolicitudContratoDTO resultado = solicitudContratoService.crearSolicitud(dto);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getNumeroSolicitud()).startsWith("SOL-");
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(resultado.getSocioId()).isEqualTo(TEST_SOCIO_ID);
        assertThat(resultado.getPetroleraId()).isEqualTo(TEST_PETROLERA_ID);

        // Verify it's persisted
        assertThat(solicitudContratoRepository.findById(resultado.getId())).isPresent();
    }

    @Test
    @DisplayName("Flujo completo - Cambiar estado BORRADOR a ENVIADO_SOCIO")
    void flujoCompleto_CambiarEstadoAEnviadoSocio() throws Exception {
        // Given - Create solicitud
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto.setSolicitadoPor("Test User");
        dto.setEsAutonomo(false);
        dto.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        byte[] mockPdf = "%PDF-1.4\nTest PDF".getBytes();
        when(petrolerasClient.tienePlantillaPdf(anyLong())).thenReturn(false);

        SolicitudContratoDTO solicitud = solicitudContratoService.crearSolicitud(dto);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);

        // When - Change to ENVIADO_SOCIO
        SolicitudContratoDTO actualizado = solicitudContratoService.cambiarEstado(
                solicitud.getId(),
                EstadoSolicitud.ENVIADO_SOCIO
        );

        // Then
        assertThat(actualizado.getEstado()).isEqualTo(EstadoSolicitud.ENVIADO_SOCIO);
        assertThat(actualizado.getFechaEnvioSocio()).isNotNull();
    }

    @Test
    @DisplayName("Flujo completo - No debe permitir transición inválida de estados")
    void flujoCompleto_NoDebePermitirTransicionInvalida() throws Exception {
        // Given
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto.setSolicitadoPor("Test User");
        dto.setEsAutonomo(false);
        dto.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        when(petrolerasClient.tienePlantillaPdf(anyLong())).thenReturn(false);

        SolicitudContratoDTO solicitud = solicitudContratoService.crearSolicitud(dto);

        // When & Then - Try to skip from BORRADOR to ENVIADO_PETROLERA
        assertThatThrownBy(() ->
                solicitudContratoService.cambiarEstado(solicitud.getId(), EstadoSolicitud.ENVIADO_PETROLERA)
        ).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Transición de estado inválida");
    }

    @Test
    @DisplayName("Flujo completo - Generar número de solicitud único y secuencial")
    void flujoCompleto_GenerarNumeroSolicitudUnico() throws Exception {
        // Given
        CrearSolicitudDTO dto1 = new CrearSolicitudDTO();
        dto1.setSocioId(TEST_SOCIO_ID);
        dto1.setPetroleraId(TEST_PETROLERA_ID);
        dto1.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto1.setSolicitadoPor("User 1");
        dto1.setEsAutonomo(false);
        dto1.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        CrearSolicitudDTO dto2 = new CrearSolicitudDTO();
        dto2.setSocioId(TEST_SOCIO_ID + 1);
        dto2.setPetroleraId(TEST_PETROLERA_ID);
        dto2.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto2.setSolicitadoPor("User 2");
        dto2.setEsAutonomo(false);
        dto2.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        when(petrolerasClient.tienePlantillaPdf(anyLong())).thenReturn(false);

        // When
        SolicitudContratoDTO solicitud1 = solicitudContratoService.crearSolicitud(dto1);
        SolicitudContratoDTO solicitud2 = solicitudContratoService.crearSolicitud(dto2);

        // Then
        assertThat(solicitud1.getNumeroSolicitud()).isNotEqualTo(solicitud2.getNumeroSolicitud());
        assertThat(solicitud1.getNumeroSolicitud()).matches("SOL-\\d{4}-\\d{5}");
        assertThat(solicitud2.getNumeroSolicitud()).matches("SOL-\\d{4}-\\d{5}");
    }

    @Test
    @DisplayName("Flujo completo - Obtener solicitud por número")
    void flujoCompleto_ObtenerSolicitudPorNumero() throws Exception {
        // Given
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(TEST_SOCIO_ID);
        dto.setPetroleraId(TEST_PETROLERA_ID);
        dto.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        dto.setSolicitadoPor("Test User");
        dto.setEsAutonomo(false);
        dto.setTipoSolicitud(TipoSolicitudContrato.NUEVO);

        when(petrolerasClient.tienePlantillaPdf(anyLong())).thenReturn(false);

        SolicitudContratoDTO creada = solicitudContratoService.crearSolicitud(dto);

        // When
        SolicitudContratoDTO encontrada = solicitudContratoService
                .obtenerPorNumeroSolicitud(creada.getNumeroSolicitud());

        // Then
        assertThat(encontrada).isNotNull();
        assertThat(encontrada.getId()).isEqualTo(creada.getId());
        assertThat(encontrada.getNumeroSolicitud()).isEqualTo(creada.getNumeroSolicitud());
    }
}
