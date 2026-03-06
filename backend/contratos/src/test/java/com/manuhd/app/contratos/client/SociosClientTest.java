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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SociosClient Unit Tests")
class SociosClientTest extends TestBase {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SociosClient sociosClient;

    private static final String BASE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sociosClient, "sociosBaseUrl", BASE_URL);
    }

    @Test
    @DisplayName("obtenerSocio - Debe retornar socio cuando existe")
    void obtenerSocio_DebeRetornarSocioCuandoExiste() {
        // Given
        Long socioId = 1L;
        SociosClient.SocioDTO dto = new SociosClient.SocioDTO();
        dto.setId(socioId);
        dto.setNumeroSocio("SOC-001");
        dto.setNombre("Juan Pérez");
        dto.setNif("12345678A");
        dto.setEmail("juan@example.com");
        dto.setTelefono("123456789");
        dto.setDireccion("Calle Test 123");
        dto.setCodigoPostal("28001");
        dto.setLocalidad("Madrid");
        dto.setProvincia("Madrid");
        dto.setActivo(true);

        ResponseEntity<SociosClient.SocioDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenReturn(response);

        // When
        SociosClient.SocioDTO resultado = sociosClient.obtenerSocio(socioId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(socioId);
        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez");
        assertThat(resultado.getEmail()).isEqualTo("juan@example.com");
        assertThat(resultado.getNumeroSocio()).isEqualTo("SOC-001");
        assertThat(resultado.getActivo()).isTrue();

        verify(restTemplate, times(1)).getForEntity(
                eq(BASE_URL + "/api/socios/" + socioId),
                eq(SociosClient.SocioDTO.class)
        );
    }

    @Test
    @DisplayName("obtenerSocio - Debe lanzar RuntimeException cuando respuesta no es exitosa")
    void obtenerSocio_DebeLanzarRuntimeExceptionCuandoRespuestaNoExitosa() {
        // Given
        Long socioId = 999L;
        ResponseEntity<SociosClient.SocioDTO> response =
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> sociosClient.obtenerSocio(socioId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo obtener información del socio");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(SociosClient.SocioDTO.class));
    }

    @Test
    @DisplayName("obtenerSocio - Debe lanzar RuntimeException cuando RestTemplate falla")
    void obtenerSocio_DebeLanzarRuntimeExceptionCuandoRestTemplateFalla() {
        // Given
        Long socioId = 1L;
        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> sociosClient.obtenerSocio(socioId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al comunicarse con el microservicio de Socios");

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(SociosClient.SocioDTO.class));
    }

    @Test
    @DisplayName("obtenerSocio - Debe lanzar RuntimeException cuando body es null")
    void obtenerSocio_DebeLanzarRuntimeExceptionCuandoBodyEsNull() {
        // Given
        Long socioId = 1L;
        ResponseEntity<SociosClient.SocioDTO> response = ResponseEntity.ok(null);

        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> sociosClient.obtenerSocio(socioId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo obtener información del socio");
    }

    @Test
    @DisplayName("obtenerSocio - Debe funcionar correctamente con todos los campos poblados")
    void obtenerSocio_DebeFuncionarCorrectamenteConTodosLosCampos() {
        // Given
        Long socioId = 1L;
        SociosClient.SocioDTO dto = new SociosClient.SocioDTO();
        dto.setId(socioId);
        dto.setNumeroSocio("SOC-12345");
        dto.setNombre("María García López");
        dto.setNif("87654321B");
        dto.setEmail("maria.garcia@example.com");
        dto.setTelefono("987654321");
        dto.setDireccion("Avenida Principal 456, 3º B");
        dto.setCodigoPostal("08001");
        dto.setLocalidad("Barcelona");
        dto.setProvincia("Barcelona");
        dto.setActivo(true);

        ResponseEntity<SociosClient.SocioDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenReturn(response);

        // When
        SociosClient.SocioDTO resultado = sociosClient.obtenerSocio(socioId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(socioId);
        assertThat(resultado.getNumeroSocio()).isEqualTo("SOC-12345");
        assertThat(resultado.getNombre()).isEqualTo("María García López");
        assertThat(resultado.getNif()).isEqualTo("87654321B");
        assertThat(resultado.getEmail()).isEqualTo("maria.garcia@example.com");
        assertThat(resultado.getTelefono()).isEqualTo("987654321");
        assertThat(resultado.getDireccion()).isEqualTo("Avenida Principal 456, 3º B");
        assertThat(resultado.getCodigoPostal()).isEqualTo("08001");
        assertThat(resultado.getLocalidad()).isEqualTo("Barcelona");
        assertThat(resultado.getProvincia()).isEqualTo("Barcelona");
        assertThat(resultado.getActivo()).isTrue();
    }

    @Test
    @DisplayName("obtenerSocio - Debe funcionar con socio inactivo")
    void obtenerSocio_DebeFuncionarConSocioInactivo() {
        // Given
        Long socioId = 2L;
        SociosClient.SocioDTO dto = new SociosClient.SocioDTO();
        dto.setId(socioId);
        dto.setNumeroSocio("SOC-999");
        dto.setNombre("Socio Inactivo");
        dto.setActivo(false);

        ResponseEntity<SociosClient.SocioDTO> response = ResponseEntity.ok(dto);

        when(restTemplate.getForEntity(anyString(), eq(SociosClient.SocioDTO.class)))
                .thenReturn(response);

        // When
        SociosClient.SocioDTO resultado = sociosClient.obtenerSocio(socioId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getActivo()).isFalse();
    }
}
