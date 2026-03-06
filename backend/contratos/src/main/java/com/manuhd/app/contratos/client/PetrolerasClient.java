package com.manuhd.app.contratos.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PetrolerasClient {

    private final RestTemplate restTemplate;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    /**
     * Obtiene el PDF de la plantilla de un tipo de solicitud desde el microservicio de Petroleras
     */
    public byte[] obtenerPlantillaPdf(Long tipoSolicitudId) throws IOException {
        String url = petrolerasBaseUrl + "/api/tipos-solicitud/" + tipoSolicitudId + "/plantilla";

        log.info("Obteniendo plantilla PDF desde: {}", url);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Plantilla PDF obtenida exitosamente, tamaño: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                throw new IOException("No se pudo obtener la plantilla PDF, status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error al obtener plantilla PDF del tipo de solicitud {}", tipoSolicitudId, e);
            throw new IOException("Error al comunicarse con el microservicio de Petroleras: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un tipo de solicitud tiene plantilla PDF configurada
     */
    public boolean tienePlantillaPdf(Long tipoSolicitudId) {
        try {
            TipoSolicitudDTO tipoSolicitud = obtenerTipoSolicitud(tipoSolicitudId);
            return tipoSolicitud != null && tipoSolicitud.getRutaPlantillaPdf() != null
                && !tipoSolicitud.getRutaPlantillaPdf().isEmpty();
        } catch (Exception e) {
            log.warn("Error al verificar si tipo de solicitud {} tiene plantilla", tipoSolicitudId, e);
            return false;
        }
    }

    /**
     * Obtiene la información de un tipo de solicitud desde el microservicio de Petroleras
     */
    public TipoSolicitudDTO obtenerTipoSolicitud(Long tipoSolicitudId) {
        String url = petrolerasBaseUrl + "/api/tipos-solicitud/" + tipoSolicitudId;

        log.info("Obteniendo tipo de solicitud desde: {}", url);

        try {
            ResponseEntity<TipoSolicitudDTO> response = restTemplate.getForEntity(url, TipoSolicitudDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Tipo de solicitud obtenido: '{}'", response.getBody().getNombre());
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error al obtener tipo de solicitud {}: {}", tipoSolicitudId, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la información de una petrolera desde el microservicio de Petroleras
     */
    public PetroleraDTO obtenerPetrolera(Long petroleraId) {
        String url = petrolerasBaseUrl + "/api/petroleras/" + petroleraId;

        log.info("Obteniendo información de la petrolera desde: {}", url);

        try {
            ResponseEntity<PetroleraDTO> response = restTemplate.getForEntity(url, PetroleraDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Información de la petrolera obtenida exitosamente: {}", response.getBody().getNombre());
                return response.getBody();
            } else {
                throw new RuntimeException("No se pudo obtener información de la petrolera, status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error al obtener información de la petrolera {}", petroleraId, e);
            throw new RuntimeException("Error al comunicarse con el microservicio de Petroleras: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene las plantillas de correo de una petrolera filtradas por tipo
     */
    public PlantillaCorreoDTO obtenerPlantillaCorreo(Long petroleraId, String tipoPlantilla) {
        String url = petrolerasBaseUrl + "/api/plantillas-correo/petrolera/" + petroleraId;

        log.info("Obteniendo plantillas de correo desde: {}", url);

        try {
            ResponseEntity<PlantillaCorreoDTO[]> response = restTemplate.getForEntity(url, PlantillaCorreoDTO[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (PlantillaCorreoDTO plantilla : response.getBody()) {
                    if (tipoPlantilla.equals(plantilla.getTipoPlantilla()) && Boolean.TRUE.equals(plantilla.getActiva())) {
                        log.info("Plantilla de correo encontrada para tipo: {}", tipoPlantilla);
                        return plantilla;
                    }
                }
            }
            log.info("No se encontró plantilla activa de tipo {} para petrolera {}", tipoPlantilla, petroleraId);
            return null;
        } catch (Exception e) {
            log.warn("Error al obtener plantilla de correo: {}", e.getMessage());
            return null;
        }
    }

    // DTOs internos para deserializar las respuestas
    @lombok.Data
    public static class PlantillaCorreoDTO {
        private Long id;
        private Long petroleraId;
        private String petroleraNombre;
        private String tipoPlantilla;
        private String asunto;
        private String cuerpo;
        private String variablesDisponibles;
        private Boolean activa;
    }

    @lombok.Data
    public static class TipoSolicitudDTO {
        private Long id;
        private Long petroleraId;
        private String nombre;
        private String codigo;
        private String descripcion;
        private Integer orden;
        private Boolean activa;
        private String rutaPlantillaPdf;
        private String nombreArchivoPlantilla;
    }

    @lombok.Data
    public static class PetroleraDTO {
        private Long id;
        private String nombre;
        private String nif;
        private String email;
        private String telefono;
        private String direccion;
        private Boolean activa;
    }
}
