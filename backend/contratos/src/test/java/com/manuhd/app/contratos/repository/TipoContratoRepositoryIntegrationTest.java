package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.TestBase;
import com.manuhd.app.contratos.model.TipoContrato;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TipoContratoRepository Integration Tests")
class TipoContratoRepositoryIntegrationTest extends TestBase {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TipoContratoRepository tipoContratoRepository;

    private TipoContrato tipoActivo;
    private TipoContrato tipoInactivo;

    @BeforeEach
    void setUp() {
        // Clear database
        tipoContratoRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test data
        tipoActivo = createTipoContrato("TIPO_ACTIVO", "Tipo Activo");
        tipoActivo.setActivo(true);
        entityManager.persist(tipoActivo);

        tipoInactivo = createTipoContrato("TIPO_INACTIVO", "Tipo Inactivo");
        tipoInactivo.setActivo(false);
        entityManager.persist(tipoInactivo);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findAll - Debe retornar todos los tipos de contrato")
    void findAll_DebeRetornarTodosLosTipos() {
        // When
        List<TipoContrato> tipos = tipoContratoRepository.findAll();

        // Then
        assertThat(tipos).hasSize(2);
        assertThat(tipos).extracting("codigo")
                .containsExactlyInAnyOrder("TIPO_ACTIVO", "TIPO_INACTIVO");
    }

    @Test
    @DisplayName("findByActivoTrue - Debe retornar solo tipos activos")
    void findByActivoTrue_DebeRetornarSoloTiposActivos() {
        // When
        List<TipoContrato> tipos = tipoContratoRepository.findByActivoTrue();

        // Then
        assertThat(tipos).hasSize(1);
        assertThat(tipos.get(0).getCodigo()).isEqualTo("TIPO_ACTIVO");
        assertThat(tipos.get(0).getActivo()).isTrue();
    }

    @Test
    @DisplayName("findByCodigo - Debe retornar tipo por código")
    void findByCodigo_DebeRetornarTipoPorCodigo() {
        // When
        Optional<TipoContrato> resultado = tipoContratoRepository.findByCodigo("TIPO_ACTIVO");

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Tipo Activo");
    }

    @Test
    @DisplayName("findByCodigo - Debe retornar empty cuando no existe")
    void findByCodigo_DebeRetornarEmptyCuandoNoExiste() {
        // When
        Optional<TipoContrato> resultado = tipoContratoRepository.findByCodigo("INEXISTENTE");

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("existsByCodigo - Debe retornar true cuando existe")
    void existsByCodigo_DebeRetornarTrueCuandoExiste() {
        // When
        boolean existe = tipoContratoRepository.existsByCodigo("TIPO_ACTIVO");

        // Then
        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsByCodigo - Debe retornar false cuando no existe")
    void existsByCodigo_DebeRetornarFalseCuandoNoExiste() {
        // When
        boolean existe = tipoContratoRepository.existsByCodigo("INEXISTENTE");

        // Then
        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("save - Debe guardar nuevo tipo de contrato")
    void save_DebeGuardarNuevoTipo() {
        // Given
        TipoContrato nuevoTipo = createTipoContrato("NUEVO_TIPO", "Nuevo Tipo");
        nuevoTipo.setActivo(true);

        // When
        TipoContrato guardado = tipoContratoRepository.save(nuevoTipo);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCodigo()).isEqualTo("NUEVO_TIPO");
        assertThat(guardado.getFechaCreacion()).isNotNull();

        // Verify it's in database
        Optional<TipoContrato> encontrado = tipoContratoRepository.findById(guardado.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("NUEVO_TIPO");
    }

    @Test
    @DisplayName("save - Debe actualizar tipo existente")
    void save_DebeActualizarTipoExistente() {
        // Given
        TipoContrato tipo = tipoContratoRepository.findById(tipoActivo.getId()).orElseThrow();
        tipo.setNombre("Nombre Actualizado");
        tipo.setDescripcion("Nueva descripción");

        // When
        TipoContrato actualizado = tipoContratoRepository.save(tipo);
        entityManager.flush();
        entityManager.clear();

        // Then
        TipoContrato verificado = tipoContratoRepository.findById(actualizado.getId()).orElseThrow();
        assertThat(verificado.getNombre()).isEqualTo("Nombre Actualizado");
        assertThat(verificado.getDescripcion()).isEqualTo("Nueva descripción");
        assertThat(verificado.getFechaActualizacion()).isNotNull();
    }

    @Test
    @DisplayName("deleteById - Debe eliminar tipo de contrato")
    void deleteById_DebeEliminarTipo() {
        // Given
        Long id = tipoInactivo.getId();

        // When
        tipoContratoRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<TipoContrato> encontrado = tipoContratoRepository.findById(id);
        assertThat(encontrado).isEmpty();

        // Verify total count
        List<TipoContrato> todos = tipoContratoRepository.findAll();
        assertThat(todos).hasSize(1);
    }

    @Test
    @DisplayName("save - Debe respetar constraint unique en código")
    void save_DebeRespetarConstraintUniqueCodigo() {
        // Given
        TipoContrato tipoDuplicado = createTipoContrato("TIPO_ACTIVO", "Tipo Duplicado");

        // When & Then
        assertThatThrownBy(() -> {
            tipoContratoRepository.save(tipoDuplicado);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("findById - Debe retornar tipo por ID")
    void findById_DebeRetornarTipoPorId() {
        // When
        Optional<TipoContrato> resultado = tipoContratoRepository.findById(tipoActivo.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("TIPO_ACTIVO");
    }

    @Test
    @DisplayName("findById - Debe retornar empty para ID inexistente")
    void findById_DebeRetornarEmptyParaIdInexistente() {
        // When
        Optional<TipoContrato> resultado = tipoContratoRepository.findById(999L);

        // Then
        assertThat(resultado).isEmpty();
    }
}
