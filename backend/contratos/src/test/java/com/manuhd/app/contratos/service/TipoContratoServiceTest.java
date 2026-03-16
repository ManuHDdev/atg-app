package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.TestBase;
import com.manuhd.app.contratos.dto.TipoContratoDTO;
import com.manuhd.app.contratos.exception.DuplicateResourceException;
import com.manuhd.app.contratos.exception.ResourceNotFoundException;
import com.manuhd.app.contratos.model.TipoContrato;
import com.manuhd.app.contratos.repository.TipoContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoContratoService Unit Tests")
class TipoContratoServiceTest extends TestBase {

    @Mock
    private TipoContratoRepository tipoContratoRepository;

    @InjectMocks
    private TipoContratoService tipoContratoService;

    private TipoContrato tipoContratoActivo;
    private TipoContrato tipoContratoInactivo;

    @BeforeEach
    void setUp() {
        tipoContratoActivo = createTipoContrato("TIPO_01", "Tipo de Contrato 1");
        tipoContratoActivo.setId(1L);
        tipoContratoActivo.setActivo(true);

        tipoContratoInactivo = createTipoContrato("TIPO_02", "Tipo de Contrato 2");
        tipoContratoInactivo.setId(2L);
        tipoContratoInactivo.setActivo(false);
    }

    @Test
    @DisplayName("listarTodos - Debe retornar todos los tipos de contrato")
    void listarTodos_DebeRetornarTodosLosTipos() {
        // Given
        List<TipoContrato> tipos = Arrays.asList(tipoContratoActivo, tipoContratoInactivo);
        when(tipoContratoRepository.findAll()).thenReturn(tipos);

        // When
        List<TipoContratoDTO> resultado = tipoContratoService.listarTodos();

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting("codigo")
                .containsExactlyInAnyOrder("TIPO_01", "TIPO_02");
        verify(tipoContratoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listarTodos - Debe retornar lista vacía cuando no hay tipos")
    void listarTodos_DebeRetornarListaVacia() {
        // Given
        when(tipoContratoRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<TipoContratoDTO> resultado = tipoContratoService.listarTodos();

        // Then
        assertThat(resultado).isEmpty();
        verify(tipoContratoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listarActivos - Debe retornar solo tipos activos")
    void listarActivos_DebeRetornarSoloTiposActivos() {
        // Given
        List<TipoContrato> tiposActivos = Arrays.asList(tipoContratoActivo);
        when(tipoContratoRepository.findByActivoTrue()).thenReturn(tiposActivos);

        // When
        List<TipoContratoDTO> resultado = tipoContratoService.listarActivos();

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("TIPO_01");
        assertThat(resultado.get(0).getActivo()).isTrue();
        verify(tipoContratoRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("obtenerPorId - Debe retornar tipo cuando existe")
    void obtenerPorId_DebeRetornarTipoCuandoExiste() {
        // Given
        Long id = 1L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));

        // When
        TipoContratoDTO resultado = tipoContratoService.obtenerPorId(id);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getCodigo()).isEqualTo("TIPO_01");
        verify(tipoContratoRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("obtenerPorId - Debe lanzar excepción cuando no existe")
    void obtenerPorId_DebeLanzarExcepcionCuandoNoExiste() {
        // Given
        Long id = 999L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.obtenerPorId(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("ID");
        verify(tipoContratoRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("obtenerPorCodigo - Debe retornar tipo cuando existe")
    void obtenerPorCodigo_DebeRetornarTipoCuandoExiste() {
        // Given
        String codigo = "TIPO_01";
        when(tipoContratoRepository.findByCodigo(codigo)).thenReturn(Optional.of(tipoContratoActivo));

        // When
        TipoContratoDTO resultado = tipoContratoService.obtenerPorCodigo(codigo);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getCodigo()).isEqualTo(codigo);
        verify(tipoContratoRepository, times(1)).findByCodigo(codigo);
    }

    @Test
    @DisplayName("obtenerPorCodigo - Debe lanzar excepción cuando no existe")
    void obtenerPorCodigo_DebeLanzarExcepcionCuandoNoExiste() {
        // Given
        String codigo = "TIPO_INEXISTENTE";
        when(tipoContratoRepository.findByCodigo(codigo)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.obtenerPorCodigo(codigo))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("código");
        verify(tipoContratoRepository, times(1)).findByCodigo(codigo);
    }

    @Test
    @DisplayName("crear - Debe crear tipo de contrato exitosamente")
    void crear_DebeCrearTipoExitosamente() {
        // Given
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("NUEVO_TIPO");
        dto.setNombre("Nuevo Tipo de Contrato");
        dto.setDescripcion("Descripción del nuevo tipo");
        dto.setActivo(true);

        TipoContrato tipoGuardado = createTipoContrato("NUEVO_TIPO", "Nuevo Tipo de Contrato");
        tipoGuardado.setId(3L);

        when(tipoContratoRepository.existsByCodigo(dto.getCodigo())).thenReturn(false);
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoGuardado);

        // When
        TipoContratoDTO resultado = tipoContratoService.crear(dto);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(3L);
        assertThat(resultado.getCodigo()).isEqualTo("NUEVO_TIPO");
        verify(tipoContratoRepository, times(1)).existsByCodigo(dto.getCodigo());
        verify(tipoContratoRepository, times(1)).save(any(TipoContrato.class));
    }

    @Test
    @DisplayName("crear - Debe lanzar excepción cuando código ya existe")
    void crear_DebeLanzarExcepcionCuandoCodigoExiste() {
        // Given
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("TIPO_01");
        dto.setNombre("Tipo Duplicado");

        when(tipoContratoRepository.existsByCodigo(dto.getCodigo())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.crear(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("código");
        verify(tipoContratoRepository, times(1)).existsByCodigo(dto.getCodigo());
        verify(tipoContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar - Debe actualizar tipo exitosamente")
    void actualizar_DebeActualizarTipoExitosamente() {
        // Given
        Long id = 1L;
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("TIPO_01"); // Mismo código
        dto.setNombre("Nombre Actualizado");
        dto.setDescripcion("Descripción actualizada");
        dto.setActivo(true);

        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoContratoActivo);

        // When
        TipoContratoDTO resultado = tipoContratoService.actualizar(id, dto);

        // Then
        assertThat(resultado).isNotNull();
        verify(tipoContratoRepository, times(1)).findById(id);
        verify(tipoContratoRepository, times(1)).save(any(TipoContrato.class));

        ArgumentCaptor<TipoContrato> captor = ArgumentCaptor.forClass(TipoContrato.class);
        verify(tipoContratoRepository).save(captor.capture());
        TipoContrato actualizado = captor.getValue();
        assertThat(actualizado.getNombre()).isEqualTo("Nombre Actualizado");
        assertThat(actualizado.getDescripcion()).isEqualTo("Descripción actualizada");
    }

    @Test
    @DisplayName("actualizar - Debe permitir cambiar código si no existe")
    void actualizar_DebePermitirCambiarCodigoSiNoExiste() {
        // Given
        Long id = 1L;
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("TIPO_NUEVO"); // Código diferente
        dto.setNombre("Nombre Actualizado");
        dto.setActivo(true);

        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));
        when(tipoContratoRepository.existsByCodigo("TIPO_NUEVO")).thenReturn(false);
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoContratoActivo);

        // When
        TipoContratoDTO resultado = tipoContratoService.actualizar(id, dto);

        // Then
        assertThat(resultado).isNotNull();
        verify(tipoContratoRepository, times(1)).existsByCodigo("TIPO_NUEVO");
        verify(tipoContratoRepository, times(1)).save(any(TipoContrato.class));
    }

    @Test
    @DisplayName("actualizar - Debe lanzar excepción cuando nuevo código existe")
    void actualizar_DebeLanzarExcepcionCuandoNuevoCodigoExiste() {
        // Given
        Long id = 1L;
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("TIPO_02"); // Código que ya existe
        dto.setNombre("Nombre Actualizado");

        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));
        when(tipoContratoRepository.existsByCodigo("TIPO_02")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.actualizar(id, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("código");
        verify(tipoContratoRepository, times(1)).existsByCodigo("TIPO_02");
        verify(tipoContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar - Debe lanzar excepción cuando tipo no existe")
    void actualizar_DebeLanzarExcepcionCuandoTipoNoExiste() {
        // Given
        Long id = 999L;
        TipoContratoDTO dto = new TipoContratoDTO();
        dto.setCodigo("TIPO_01");
        dto.setNombre("Nombre");

        when(tipoContratoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.actualizar(id, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("ID");
        verify(tipoContratoRepository, times(1)).findById(id);
        verify(tipoContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar - Debe eliminar tipo exitosamente (borrado lógico)")
    void eliminar_DebeEliminarTipoExitosamente() {
        // Given
        Long id = 1L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoContratoActivo);

        // When
        tipoContratoService.eliminar(id);

        // Then
        verify(tipoContratoRepository, times(1)).findById(id);
        ArgumentCaptor<TipoContrato> captor = ArgumentCaptor.forClass(TipoContrato.class);
        verify(tipoContratoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("eliminar - Debe lanzar excepción cuando tipo no existe")
    void eliminar_DebeLanzarExcepcionCuandoTipoNoExiste() {
        // Given
        Long id = 999L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.eliminar(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("ID");
        verify(tipoContratoRepository, times(1)).findById(id);
        verify(tipoContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("activar - Debe activar tipo exitosamente")
    void activar_DebeActivarTipoExitosamente() {
        // Given
        Long id = 2L;
        tipoContratoInactivo.setActivo(false);
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoInactivo));
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoContratoInactivo);

        // When
        tipoContratoService.activar(id);

        // Then
        verify(tipoContratoRepository, times(1)).findById(id);
        ArgumentCaptor<TipoContrato> captor = ArgumentCaptor.forClass(TipoContrato.class);
        verify(tipoContratoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    @DisplayName("desactivar - Debe desactivar tipo exitosamente")
    void desactivar_DebeDesactivarTipoExitosamente() {
        // Given
        Long id = 1L;
        tipoContratoActivo.setActivo(true);
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.of(tipoContratoActivo));
        when(tipoContratoRepository.save(any(TipoContrato.class))).thenReturn(tipoContratoActivo);

        // When
        tipoContratoService.desactivar(id);

        // Then
        verify(tipoContratoRepository, times(1)).findById(id);
        ArgumentCaptor<TipoContrato> captor = ArgumentCaptor.forClass(TipoContrato.class);
        verify(tipoContratoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
    }

    @Test
    @DisplayName("activar - Debe lanzar excepción cuando tipo no existe")
    void activar_DebeLanzarExcepcionCuandoTipoNoExiste() {
        // Given
        Long id = 999L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.activar(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("ID");
        verify(tipoContratoRepository, times(1)).findById(id);
        verify(tipoContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivar - Debe lanzar excepción cuando tipo no existe")
    void desactivar_DebeLanzarExcepcionCuandoTipoNoExiste() {
        // Given
        Long id = 999L;
        when(tipoContratoRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tipoContratoService.desactivar(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de contrato")
                .hasMessageContaining("ID");
        verify(tipoContratoRepository, times(1)).findById(id);
        verify(tipoContratoRepository, never()).save(any());
    }
}
