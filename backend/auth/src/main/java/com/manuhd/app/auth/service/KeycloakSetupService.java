package com.manuhd.app.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KeycloakSetupService {

    @Value("${keycloak.internal-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Value("${keycloak.developer.username:admin}")
    private String developerUsername;

    private final RestTemplate restTemplate = new RestTemplate();

    @EventListener(ApplicationReadyEvent.class)
    public void setupDeveloperRole() {
        try {
            String token = getAdminToken();
            crearRolSiNoExiste(token, "DEVELOPER", "Desarrollador. Gestiona incidencias y estados.");
            asignarRolAUsuario(token, developerUsername, "DEVELOPER");
        } catch (Exception e) {
            // No fallar el arranque si Keycloak no está disponible
        }
    }

    private String getAdminToken() {
        String tokenUrl = keycloakUrl + "/keycloak/realms/master/protocol/openid-connect/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        return (String) Objects.requireNonNull(response.getBody()).get("access_token");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private void crearRolSiNoExiste(String token, String roleName, String description) {
        String rolesUrl = keycloakUrl + "/keycloak/admin/realms/" + realm + "/roles";
        try {
            restTemplate.exchange(rolesUrl + "/" + roleName, HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), Map.class);
            // Role ya existe
        } catch (HttpClientErrorException.NotFound e) {
            Map<String, String> role = Map.of("name", roleName, "description", description);
            restTemplate.exchange(rolesUrl, HttpMethod.POST,
                    new HttpEntity<>(role, authHeaders(token)), Void.class);
        }
    }

    @SuppressWarnings("unchecked")
    private void asignarRolAUsuario(String token, String username, String roleName) {
        String usersUrl = keycloakUrl + "/keycloak/admin/realms/" + realm + "/users";

        // Buscar usuario por username
        ResponseEntity<List<Map<String, Object>>> usersResp = restTemplate.exchange(
                usersUrl + "?username=" + username + "&exact=true", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                (Class<List<Map<String, Object>>>) (Class<?>) List.class);

        List<Map<String, Object>> users = usersResp.getBody();
        if (users == null || users.isEmpty()) return;

        String userId = (String) users.get(0).get("id");

        // Verificar si ya tiene el rol
        ResponseEntity<List<Map<String, Object>>> rolesResp = restTemplate.exchange(
                usersUrl + "/" + userId + "/role-mappings/realm", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                (Class<List<Map<String, Object>>>) (Class<?>) List.class);

        boolean yaAsignado = Objects.requireNonNull(rolesResp.getBody()).stream()
                .anyMatch(r -> roleName.equals(r.get("name")));
        if (yaAsignado) return;

        // Obtener representación del rol y asignarlo
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> roleResp = restTemplate.exchange(
                keycloakUrl + "/keycloak/admin/realms/" + realm + "/roles/" + roleName,
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        restTemplate.exchange(
                usersUrl + "/" + userId + "/role-mappings/realm", HttpMethod.POST,
                new HttpEntity<>(List.of(roleResp.getBody()), authHeaders(token)), Void.class);
    }
}
