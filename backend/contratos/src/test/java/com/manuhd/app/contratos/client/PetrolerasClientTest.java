package com.manuhd.app.contratos.client;

import com.manuhd.app.contratos.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetrolerasClient Unit Tests")
class PetrolerasClientTest extends TestBase {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PetrolerasClient petrolerasClient;

    private static final String BASE_URL = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(petrolerasClient, "petrolerasBaseUrl", BASE_URL);
    }

    @Test
    @DisplayName("obtenerPlantillaPdf - Debe retornar PDF cuando existe")
    void obtenerPlantillaPdf_DebeRetornarPdfCuandoExiste() throws IOException {
        // Given
        Long tipoSolicitudId = 1L;
        byte[] pdfContent = "%PDF-1.4\nTest PDF".getBytes();
        ResponseEntity<byte[]> response = ResponseEntity.ok(pdfContent);

        when(restTemplate.getForEntity(anyString(), eq(byte[].class))).thenReturn(response);

        // When
        byte[] resultado = petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEqualTo(pdfContent);
        assertThat(resultado.length).isGreaterThan(0);

        verify(restTemplate, times(1)).getForEntity(
                eq(BASE_URL + "/api/tipos-solicitud/" + tipoSolicitudId + "/plantilla"),
                eq(byte[].class)
        );
    }

    @Test
    @DisplayName("obtenerPlantillaPdf - Debe lanzar IOException cuando respuesta no es exitosa")
    void obtenerPlantillaPdf_DebeLanzarIOExceptionCuandoRespuestaNoExitosa() {
        // Given
        Long tipoSolicitudId = 1L;
        ResponseEntity<byte[]> response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        when(restTemplate.getForEntity(anyString(), eq(byte[].class))).thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No se pudo obtener la plantilla PDF");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("obtenerPlantillaPdf - Debe lanzar IOException cuando RestTemplate lanza excepción")
    void obtenerPlantillaPdf_DebeLanzarIOExceptionCuandoRestTemplateFalla() {
        // Given
        Long tipoSolicitudId = 1L;
        when(restTemplate.getForEntity(anyString(), eq(byte[].class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Error al comunicarse con el microservicio de Petroleras");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("obtenerPlantillaPdf - Debe lanzar IOException cuando body es null")
    void obtenerPlantillaPdf_DebeLanzarIOExceptionCuandoBodyEsNull() {
        // Given
        Long tipoSolicitudId = 1L;
        ResponseEntity<byte[]> response = ResponseEntity.ok(null);

        when(restTemplate.getForEntity(anyString(), eq(byte[].class))).thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No se pudo obtener la plantilla PDF");
    }

    @Test
    @DisplayName("tienePlantillaPdf - Debe retornar true cuando tiene plantilla")
    void tienePlantillaPdf_DebeRetornarTrueCuandoTienePlantilla() {
        // Given
        Long tipoSolicitudId = 1L;
        PetrolerasClient.TipoSolicitudDTO dto = new PetrolerasClient.TipoSolicitudDTO();
        dto.setId(tipoSolicitudId);
        dto.setRutaPlantillaPdf("/path/to/plantilla.pdf");
        ResponseEntity<PetrolerasClient.TipoSolicitudDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.TipoSolicitudDTO.class)))
                .thenReturn(response);

        // When
        boolean resultado = petrolerasClient.tienePlantillaPdf(tipoSolicitudId);

        // Then
        assertThat(resultado).isTrue();
        verify(restTemplate, times(1)).getForEntity(
                eq(BASE_URL + "/api/tipos-solicitud/" + tipoSolicitudId),
                eq(PetrolerasClient.TipoSolicitudDTO.class)
        );
    }

    @Test
    @DisplayName("tienePlantillaPdf - Debe retornar false cuando no tiene plantilla")
    void tienePlantillaPdf_DebeRetornarFalseCuandoNoTienePlantilla() {
        // Given
        Long tipoSolicitudId = 1L;
        PetrolerasClient.TipoSolicitudDTO dto = new PetrolerasClient.TipoSolicitudDTO();
        dto.setId(tipoSolicitudId);
        dto.setRutaPlantillaPdf(null);
        ResponseEntity<PetrolerasClient.TipoSolicitudDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.TipoSolicitudDTO.class)))
                .thenReturn(response);

        // When
        boolean resultado = petrolerasClient.tienePlantillaPdf(tipoSolicitudId);

        // Then
        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("tienePlantillaPdf - Debe retornar false cuando ruta está vacía")
    void tienePlantillaPdf_DebeRetornarFalseCuandoRutaEstaVacia() {
        // Given
        Long tipoSolicitudId = 1L;
        PetrolerasClient.TipoSolicitudDTO dto = new PetrolerasClient.TipoSolicitudDTO();
        dto.setId(tipoSolicitudId);
        dto.setRutaPlantillaPdf("");
        ResponseEntity<PetrolerasClient.TipoSolicitudDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.TipoSolicitudDTO.class)))
                .thenReturn(response);

        // When
        boolean resultado = petrolerasClient.tienePlantillaPdf(tipoSolicitudId);

        // Then
        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("tienePlantillaPdf - Debe retornar false cuando hay excepción")
    void tienePlantillaPdf_DebeRetornarFalseCuandoHayExcepcion() {
        // Given
        Long tipoSolicitudId = 1L;
        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.TipoSolicitudDTO.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // When
        boolean resultado = petrolerasClient.tienePlantillaPdf(tipoSolicitudId);

        // Then
        assertThat(resultado).isFalse();
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(PetrolerasClient.TipoSolicitudDTO.class));
    }

    @Test
    @DisplayName("obtenerPetrolera - Debe retornar petrolera cuando existe")
    void obtenerPetrolera_DebeRetornarPetroleraCuandoExiste() {
        // Given
        Long petroleraId = 1L;
        PetrolerasClient.PetroleraDTO dto = new PetrolerasClient.PetroleraDTO();
        dto.setId(petroleraId);
        dto.setNombre("Petrolera Test");
        dto.setNif("B12345678");
        dto.setEmail("petrolera@test.com");
        dto.setActiva(true);
        ResponseEntity<PetrolerasClient.PetroleraDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class)))
                .thenReturn(response);

        // When
        PetrolerasClient.PetroleraDTO resultado = petrolerasClient.obtenerPetrolera(petroleraId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(petroleraId);
        assertThat(resultado.getNombre()).isEqualTo("Petrolera Test");
        assertThat(resultado.getEmail()).isEqualTo("petrolera@test.com");

        verify(restTemplate, times(1)).getForEntity(
                eq(BASE_URL + "/api/petroleras/" + petroleraId),
                eq(PetrolerasClient.PetroleraDTO.class)
        );
    }

    @Test
    @DisplayName("obtenerPetrolera - Debe lanzar RuntimeException cuando respuesta no es exitosa")
    void obtenerPetrolera_DebeLanzarRuntimeExceptionCuandoRespuestaNoExitosa() {
        // Given
        Long petroleraId = 1L;
        ResponseEntity<PetrolerasClient.PetroleraDTO> response =
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class)))
                .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPetrolera(petroleraId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo obtener información de la petrolera");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class));
    }

    @Test
    @DisplayName("obtenerPetrolera - Debe lanzar RuntimeException cuando RestTemplate falla")
    void obtenerPetrolera_DebeLanzarRuntimeExceptionCuandoRestTemplateFalla() {
        // Given
        Long petroleraId = 1L;
        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPetrolera(petroleraId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al comunicarse con el microservicio de Petroleras");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class));
    }

    @Test
    @DisplayName("obtenerPetrolera - Debe lanzar RuntimeException cuando body es null")
    void obtenerPetrolera_DebeLanzarRuntimeExceptionCuandoBodyEsNull() {
        // Given
        Long petroleraId = 1L;
        ResponseEntity<PetrolerasClient.PetroleraDTO> response = ResponseEntity.ok(null);

        when(restTemplate.getForEntity(anyString(), eq(PetrolerasClient.PetroleraDTO.class)))
                .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> petrolerasClient.obtenerPetrolera(petroleraId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo obtener información de la petrolera");
    }
}
