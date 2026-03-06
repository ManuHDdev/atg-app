package com.manuhd.app.contratos.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SociosClient {

    private final RestTemplate restTemplate;

    @Value("${microservices.socios.url:http://localhost:8081}")
    private String sociosBaseUrl;

    /**
     * Obtiene la información de un socio desde el microservicio de Socios
     */
    public SocioDTO obtenerSocio(Long socioId) {
        String url = sociosBaseUrl + "/api/socios/" + socioId;

        log.info("Obteniendo información del socio desde: {}", url);

        try {
            ResponseEntity<SocioDTO> response = restTemplate.getForEntity(url, SocioDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Información del socio obtenida exitosamente: {}", response.getBody().getNombre());
                return response.getBody();
            } else {
                throw new RuntimeException("No se pudo obtener información del socio, status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error al obtener información del socio {}", socioId, e);
            throw new RuntimeException("Error al comunicarse con el microservicio de Socios: " + e.getMessage(), e);
        }
    }

    // DTO interno para deserializar la respuesta
    @Data
    public static class SocioDTO {
        private Long id;
        private String numeroSocio;
        private String nombre;
        private String nif;
        private String email;
        private String telefono;
        private String direccion;
        private String codigoPostal;
        private String localidad;
        private String provincia;
        private Boolean activo;
    }
}
