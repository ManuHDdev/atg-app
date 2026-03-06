package com.manuhd.app.auth.service;

import com.manuhd.app.auth.dto.CreateUsuarioRequest;
import com.manuhd.app.auth.dto.UpdateUsuarioRequest;
import com.manuhd.app.auth.dto.UsuarioDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class KeycloakAdminService {

    private static final List<String> APP_ROLES = List.of("ADMIN", "GESTOR", "USUARIO");

    @Value("${keycloak.internal-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedToken;
    private Instant tokenExpiry;

    private synchronized String getAdminToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
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

        Map<String, Object> body = Objects.requireNonNull(response.getBody());
        cachedToken = (String) body.get("access_token");
        int expiresIn = (int) body.get("expires_in");
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 30);
        return cachedToken;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getAdminToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String usersUrl() {
        return keycloakUrl + "/keycloak/admin/realms/" + realm + "/users";
    }

    public List<UsuarioDto> listarUsuarios() {
        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usersUrl() + "?max=200", HttpMethod.GET, new HttpEntity<>(authHeaders()),
                (Class<List<Map<String, Object>>>) (Class<?>) List.class);
        List<Map<String, Object>> users = Objects.requireNonNull(response.getBody());
        return users.stream().map(this::toUsuarioDto).toList();
    }

    public UsuarioDto obtenerUsuario(String id) {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usersUrl() + "/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders()),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        return toUsuarioDto(Objects.requireNonNull(response.getBody()));
    }

    public UsuarioDto crearUsuario(CreateUsuarioRequest req) {
        Map<String, Object> userRep = new HashMap<>();
        userRep.put("username", req.username());
        userRep.put("firstName", req.firstName());
        userRep.put("lastName", req.lastName());
        userRep.put("email", req.email());
        userRep.put("enabled", req.enabled());
        userRep.put("emailVerified", true);
        userRep.put("credentials", List.of(Map.of(
                "type", "password", "value", req.password(), "temporary", false)));

        ResponseEntity<Void> response = restTemplate.exchange(
                usersUrl(), HttpMethod.POST, new HttpEntity<>(userRep, authHeaders()), Void.class);

        String location = Objects.requireNonNull(response.getHeaders().getFirst("Location"));
        String userId = location.substring(location.lastIndexOf('/') + 1);

        if (req.role() != null && !req.role().isBlank()) {
            asignarRol(userId, req.role());
        }
        return obtenerUsuario(userId);
    }

    public UsuarioDto actualizarUsuario(String id, UpdateUsuarioRequest req) {
        Map<String, Object> userRep = new HashMap<>();
        userRep.put("firstName", req.firstName());
        userRep.put("lastName", req.lastName());
        userRep.put("email", req.email());
        userRep.put("enabled", req.enabled());
        userRep.put("emailVerified", true);
        restTemplate.exchange(usersUrl() + "/" + id, HttpMethod.PUT,
                new HttpEntity<>(userRep, authHeaders()), Void.class);

        if (req.role() != null && !req.role().isBlank()) {
            List<String> current = obtenerRolesUsuario(id);
            for (String r : current) {
                if (APP_ROLES.contains(r)) eliminarRol(id, r);
            }
            asignarRol(id, req.role());
        }

        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            resetPassword(id, req.newPassword());
        }
        return obtenerUsuario(id);
    }

    public void eliminarUsuario(String id) {
        restTemplate.exchange(usersUrl() + "/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);
    }

    private void asignarRol(String userId, String roleName) {
        Map<String, Object> roleRep = obtenerRoleRepresentation(roleName);
        restTemplate.exchange(usersUrl() + "/" + userId + "/role-mappings/realm",
                HttpMethod.POST, new HttpEntity<>(List.of(roleRep), authHeaders()), Void.class);
    }

    private void eliminarRol(String userId, String roleName) {
        try {
            Map<String, Object> roleRep = obtenerRoleRepresentation(roleName);
            restTemplate.exchange(usersUrl() + "/" + userId + "/role-mappings/realm",
                    HttpMethod.DELETE, new HttpEntity<>(List.of(roleRep), authHeaders()), Void.class);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obtenerRoleRepresentation(String roleName) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                keycloakUrl + "/keycloak/admin/realms/" + realm + "/roles/" + roleName,
                HttpMethod.GET, new HttpEntity<>(authHeaders()),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        return Objects.requireNonNull(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private List<String> obtenerRolesUsuario(String userId) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usersUrl() + "/" + userId + "/role-mappings/realm",
                HttpMethod.GET, new HttpEntity<>(authHeaders()),
                (Class<List<Map<String, Object>>>) (Class<?>) List.class);
        List<Map<String, Object>> roles = Objects.requireNonNull(response.getBody());
        return roles.stream().map(r -> (String) r.get("name")).toList();
    }

    private void resetPassword(String userId, String newPassword) {
        Map<String, Object> cred = new HashMap<>();
        cred.put("type", "password");
        cred.put("value", newPassword);
        cred.put("temporary", false);
        restTemplate.exchange(usersUrl() + "/" + userId + "/reset-password",
                HttpMethod.PUT, new HttpEntity<>(cred, authHeaders()), Void.class);
    }

    private UsuarioDto toUsuarioDto(Map<String, Object> user) {
        String id = (String) user.get("id");
        List<String> appRoles = obtenerRolesUsuario(id).stream()
                .filter(APP_ROLES::contains).toList();
        return new UsuarioDto(
                id,
                (String) user.get("username"),
                (String) user.getOrDefault("firstName", ""),
                (String) user.getOrDefault("lastName", ""),
                (String) user.getOrDefault("email", ""),
                Boolean.TRUE.equals(user.get("enabled")),
                appRoles);
    }
}
