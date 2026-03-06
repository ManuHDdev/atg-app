package com.manuhd.app.contratos.controller;

import com.manuhd.app.contratos.dto.TipoContratoDTO;
import com.manuhd.app.contratos.service.TipoContratoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TipoContratoController.class)
@DisplayName("TipoContratoController Integration Tests")
class TipoContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TipoContratoService tipoContratoService;

    @Test
    @DisplayName("GET /api/tipos-contrato - Debe retornar todos los tipos")
    void getAllTiposContrato_DebeRetornarTodosLosTipos() throws Exception {
        // Given
        TipoContratoDTO tipo1 = createTipoContratoDTO(1L, "TIPO_01", "Tipo 1", true);
        TipoContratoDTO tipo2 = createTipoContratoDTO(2L, "TIPO_02", "Tipo 2", false);
        List<TipoContratoDTO> tipos = Arrays.asList(tipo1, tipo2);

        when(tipoContratoService.listarTodos()).thenReturn(tipos);

        // When & Then
        mockMvc.perform(get("/api/tipos-contrato"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].codigo", is("TIPO_01")))
                .andExpect(jsonPath("$[0].nombre", is("Tipo 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].codigo", is("TIPO_02")));

        verify(tipoContratoService, times(1)).listarTodos();
    }

    @Test
    @DisplayName("GET /api/tipos-contrato/activos - Debe retornar solo tipos activos")
    void getActivosTiposContrato_DebeRetornarSoloTiposActivos() throws Exception {
        // Given
        TipoContratoDTO tipo1 = createTipoContratoDTO(1L, "TIPO_01", "Tipo Activo", true);
        List<TipoContratoDTO> tipos = Arrays.asList(tipo1);

        when(tipoContratoService.listarActivos()).thenReturn(tipos);

        // When & Then
        mockMvc.perform(get("/api/tipos-contrato/activos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].activo", is(true)));

        verify(tipoContratoService, times(1)).listarActivos();
    }

    @Test
    @DisplayName("GET /api/tipos-contrato/{id} - Debe retornar tipo por ID")
    void getTipoContratoById_DebeRetornarTipoPorId() throws Exception {
        // Given
        Long id = 1L;
        TipoContratoDTO tipo = createTipoContratoDTO(id, "TIPO_01", "Tipo Test", true);

        when(tipoContratoService.obtenerPorId(id)).thenReturn(tipo);

        // When & Then
        mockMvc.perform(get("/api/tipos-contrato/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.codigo", is("TIPO_01")))
                .andExpect(jsonPath("$.nombre", is("Tipo Test")));

        verify(tipoContratoService, times(1)).obtenerPorId(id);
    }

    @Test
    @DisplayName("GET /api/tipos-contrato/{id} - Debe retornar 404 cuando no existe")
    void getTipoContratoById_DebeRetornar404CuandoNoExiste() throws Exception {
        // Given
        Long id = 999L;
        when(tipoContratoService.obtenerPorId(id))
                .thenThrow(new RuntimeException("Tipo de contrato no encontrado con ID: " + id));

        // When & Then
        mockMvc.perform(get("/api/tipos-contrato/{id}", id))
                .andExpect(status().isInternalServerError());

        verify(tipoContratoService, times(1)).obtenerPorId(id);
    }

    @Test
    @DisplayName("POST /api/tipos-contrato - Debe crear tipo exitosamente")
    void createTipoContrato_DebeCrearTipoExitosamente() throws Exception {
        // Given
        TipoContratoDTO inputDTO = createTipoContratoDTO(null, "NUEVO_TIPO", "Nuevo Tipo", true);
        TipoContratoDTO savedDTO = createTipoContratoDTO(1L, "NUEVO_TIPO", "Nuevo Tipo", true);

        when(tipoContratoService.crear(any(TipoContratoDTO.class))).thenReturn(savedDTO);

        // When & Then
        mockMvc.perform(post("/api/tipos-contrato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.codigo", is("NUEVO_TIPO")))
                .andExpect(jsonPath("$.nombre", is("Nuevo Tipo")));

        verify(tipoContratoService, times(1)).crear(any(TipoContratoDTO.class));
    }

    @Test
    @DisplayName("POST /api/tipos-contrato - Debe retornar error cuando código ya existe")
    void createTipoContrato_DebeRetornarErrorCuandoCodigoExiste() throws Exception {
        // Given
        TipoContratoDTO inputDTO = createTipoContratoDTO(null, "TIPO_EXISTENTE", "Tipo Existente", true);

        when(tipoContratoService.crear(any(TipoContratoDTO.class)))
                .thenThrow(new RuntimeException("Ya existe un tipo de contrato con el código: TIPO_EXISTENTE"));

        // When & Then
        mockMvc.perform(post("/api/tipos-contrato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isInternalServerError());

        verify(tipoContratoService, times(1)).crear(any(TipoContratoDTO.class));
    }

    @Test
    @DisplayName("PUT /api/tipos-contrato/{id} - Debe actualizar tipo exitosamente")
    void updateTipoContrato_DebeActualizarTipoExitosamente() throws Exception {
        // Given
        Long id = 1L;
        TipoContratoDTO inputDTO = createTipoContratoDTO(id, "TIPO_01", "Tipo Actualizado", true);
        TipoContratoDTO updatedDTO = createTipoContratoDTO(id, "TIPO_01", "Tipo Actualizado", true);

        when(tipoContratoService.actualizar(anyLong(), any(TipoContratoDTO.class)))
                .thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(put("/api/tipos-contrato/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Tipo Actualizado")));

        verify(tipoContratoService, times(1)).actualizar(eq(id), any(TipoContratoDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/tipos-contrato/{id} - Debe eliminar tipo exitosamente")
    void deleteTipoContrato_DebeEliminarTipoExitosamente() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(tipoContratoService).eliminar(id);

        // When & Then
        mockMvc.perform(delete("/api/tipos-contrato/{id}", id))
                .andExpect(status().isOk());

        verify(tipoContratoService, times(1)).eliminar(id);
    }

    @Test
    @DisplayName("POST /api/tipos-contrato/{id}/activar - Debe activar tipo")
    void activarTipoContrato_DebeActivarTipo() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(tipoContratoService).activar(id);

        // When & Then
        mockMvc.perform(post("/api/tipos-contrato/{id}/activar", id))
                .andExpect(status().isOk());

        verify(tipoContratoService, times(1)).activar(id);
    }

    @Test
    @DisplayName("POST /api/tipos-contrato/{id}/desactivar - Debe desactivar tipo")
    void desactivarTipoContrato_DebeDesactivarTipo() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(tipoContratoService).desactivar(id);

        // When & Then
        mockMvc.perform(post("/api/tipos-contrato/{id}/desactivar", id))
                .andExpect(status().isOk());

        verify(tipoContratoService, times(1)).desactivar(id);
    }

    @Test
    @DisplayName("GET /api/tipos-contrato/codigo/{codigo} - Debe retornar tipo por código")
    void getTipoContratoByCodigo_DebeRetornarTipoPorCodigo() throws Exception {
        // Given
        String codigo = "TIPO_01";
        TipoContratoDTO tipo = createTipoContratoDTO(1L, codigo, "Tipo Test", true);

        when(tipoContratoService.obtenerPorCodigo(codigo)).thenReturn(tipo);

        // When & Then
        mockMvc.perform(get("/api/tipos-contrato/codigo/{codigo}", codigo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(codigo)))
                .andExpect(jsonPath("$.nombre", is("Tipo Test")));

        verify(tipoContratoService, times(1)).obtenerPorCodigo(codigo);
    }

    // Helper method
    private TipoContratoDTO createTipoContratoDTO(Long id, String codigo, String nombre, Boolean activo) {
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setId(id);
        dto.setCodigo(codigo);
        dto.setNombre(nombre);
        dto.setDescripcion("Descripción de " + nombre);
        dto.setActivo(activo);
        return dto;
    }
}
