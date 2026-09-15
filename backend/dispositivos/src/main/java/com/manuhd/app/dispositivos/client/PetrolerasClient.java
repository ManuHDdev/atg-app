package com.manuhd.app.dispositivos.client;

import com.manuhd.app.dispositivos.model.TipoSolicitud;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Acceso al microservicio de petroleras para el circuito del documento firmado.
 *
 * <p>Usa el {@code RestTemplate} de {@code AppConfig}, cuyo interceptor ya propaga el JWT
 * del usuario que esta operando: aqui no se monta ninguna autenticacion propia.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PetrolerasClient {

    /** Modulo con el que las plantillas de documento estan dadas de alta en petroleras. */
    private static final String MODULO_DISPOSITIVOS = "DISPOSITIVOS";

    private final RestTemplate restTemplate;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    /**
     * Descarga la plantilla PDF configurada para el tipo de solicitud indicado.
     *
     * @throws IOException con el prefijo {@code PLANTILLA_NO_CONFIGURADA:} cuando la petrolera
     *     todavia no tiene plantilla para ese tipo (404), o con el error de comunicacion en
     *     cualquier otro caso
     */
    public byte[] obtenerPlantillaDocumento(Long petroleraId, TipoSolicitud tipoSolicitud) throws IOException {
        String url = UriComponentsBuilder.fromUriString(petrolerasBaseUrl)
                .path("/api/plantillas-documento/descargar")
                .queryParam("petroleraId", petroleraId)
                .queryParam("modulo", MODULO_DISPOSITIVOS)
                .queryParam("tipoSolicitud", tipoSolicitud.name())
                .toUriString();

        log.info("Obteniendo plantilla de documento desde: {}", url);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Plantilla de documento obtenida, tamanio: {} bytes", response.getBody().length);
                return response.getBody();
            }
            throw new IOException("No se pudo obtener la plantilla PDF, status: " + response.getStatusCode());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Sin plantilla de documento para petrolera {} y tipo {}", petroleraId, tipoSolicitud);
            throw new IOException("PLANTILLA_NO_CONFIGURADA: La petrolera no tiene configurada una plantilla de "
                    + tipoSolicitud.name() + " para dispositivos. Contacte con un administrador.", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener la plantilla de documento de la petrolera {}", petroleraId, e);
            throw new IOException("Error al comunicarse con el microservicio de Petroleras: " + e.getMessage(), e);
        }
    }
}
