package com.manuhd.app.socios.controller;

import com.manuhd.app.socios.dto.EmpresaDTO;
import com.manuhd.app.socios.model.Empresa;
import com.manuhd.app.socios.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> getAllEmpresas() {
        return ResponseEntity.ok(empresaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDTO> getEmpresaById(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.findByIdDTO(id));
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<EmpresaDTO>> getEmpresasBySocioId(@PathVariable Long socioId) {
        return ResponseEntity.ok(empresaService.findBySocioId(socioId));
    }

    @PostMapping
    public ResponseEntity<EmpresaDTO> createEmpresa(@Valid @RequestBody EmpresaDTO empresaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.createFromDTO(empresaDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaDTO> updateEmpresa(@PathVariable Long id, @Valid @RequestBody EmpresaDTO empresaDTO) {
        return ResponseEntity.ok(empresaService.updateFromDTO(id, empresaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmpresa(@PathVariable Long id) {
        empresaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
