package com.manuhd.app.auth.controller;

import com.manuhd.app.auth.dto.CreateUsuarioRequest;
import com.manuhd.app.auth.dto.UpdateUsuarioRequest;
import com.manuhd.app.auth.dto.UsuarioDto;
import com.manuhd.app.auth.service.KeycloakAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final KeycloakAdminService keycloakAdminService;

    public UsuarioController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    @GetMapping
    ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(keycloakAdminService.listarUsuarios());
    }

    @PostMapping
    ResponseEntity<UsuarioDto> crear(@RequestBody CreateUsuarioRequest req) {
        return ResponseEntity.ok(keycloakAdminService.crearUsuario(req));
    }

    @PutMapping("/{id}")
    ResponseEntity<UsuarioDto> actualizar(@PathVariable String id, @RequestBody UpdateUsuarioRequest req) {
        return ResponseEntity.ok(keycloakAdminService.actualizarUsuario(id, req));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> eliminar(@PathVariable String id) {
        keycloakAdminService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
