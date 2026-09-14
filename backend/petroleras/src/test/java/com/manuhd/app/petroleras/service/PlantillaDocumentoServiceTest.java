package com.manuhd.app.petroleras.service;

import com.manuhd.app.petroleras.dto.PlantillaDocumentoDTO;
import com.manuhd.app.petroleras.enums.ModuloDocumento;
import com.manuhd.app.petroleras.exception.BusinessValidationException;
import com.manuhd.app.petroleras.exception.ResourceNotFoundException;
import com.manuhd.app.petroleras.model.Petrolera;
import com.manuhd.app.petroleras.model.PlantillaDocumento;
import com.manuhd.app.petroleras.repository.PetroleraRepository;
import com.manuhd.app.petroleras.repository.PlantillaDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas del almacén de plantillas PDF por módulo: unicidad de la clave entre plantillas
 * vivas, validación real de PDF, borrado lógico y el 404 que esperan tarjetas y dispositivos.
 * Sin contexto de Spring ni base de datos: JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlantillaDocumentoService - plantillas PDF por petrolera y módulo")
class PlantillaDocumentoServiceTest {

    private static final Long PETROLERA_ID = 20L;
    private static final Long PLANTILLA_ID = 1L;
    private static final ModuloDocumento MODULO = ModuloDocumento.TARJETAS;
    private static final String TIPO_SOLICITUD = "ALTA";

    @TempDir
    Path storageDir;

    @Mock
    private PlantillaDocumentoRepository plantillaDocumentoRepository;

    @Mock
    private PetroleraRepository petroleraRepository;

    @InjectMocks
    private PlantillaDocumentoService service;

    private Petrolera petrolera;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "plantillasBasePath", storageDir.toString());

        petrolera = new Petrolera();
        petrolera.setId(PETROLERA_ID);
        petrolera.setNombre("Repsol");
    }

    private MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("file", nombre, "application/pdf",
                "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.UTF_8));
    }

    private PlantillaDocumento plantilla() {
        return PlantillaDocumento.builder()
                .id(PLANTILLA_ID)
                .petroleraId(PETROLERA_ID)
                .modulo(MODULO)
                .tipoSolicitud(TIPO_SOLICITUD)
                .nombreArchivo("alta.pdf")
                .activa(true)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---------- crear ----------

    @Test
    @DisplayName("crear - Guarda el PDF en disco bajo petrolera y módulo y normaliza el tipo")
    void crear_ConPdfValido_GuardaArchivoYDevuelveDTO() throws Exception {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));
        when(plantillaDocumentoRepository.findByPetroleraIdAndModuloAndTipoSolicitudAndActivoTrue(
                PETROLERA_ID, MODULO, TIPO_SOLICITUD)).thenReturn(Optional.empty());
        when(plantillaDocumentoRepository.save(any(PlantillaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlantillaDocumentoDTO dto = service.crear(PETROLERA_ID, MODULO, "  alta  ", pdf("alta.pdf"));

        assertThat(dto.getTipoSolicitud()).isEqualTo("ALTA");
        assertThat(dto.getPetroleraNombre()).isEqualTo("Repsol");
        assertThat(dto.getNombreArchivo()).isEqualTo("alta.pdf");
        assertThat(dto.getActiva()).isTrue();

        Path guardado = Path.of(dto.getRutaArchivo());
        assertThat(guardado).exists();
        assertThat(guardado.getParent())
                .isEqualTo(storageDir.resolve("petrolera_" + PETROLERA_ID).resolve("tarjetas"));
        assertThat(Files.readString(guardado)).startsWith("%PDF-");
    }

    @Test
    @DisplayName("crear - Rechaza una segunda plantilla para la misma petrolera, módulo y tipo")
    void crear_ClaveDuplicada_LanzaBusinessValidation() {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));
        when(plantillaDocumentoRepository.findByPetroleraIdAndModuloAndTipoSolicitudAndActivoTrue(
                PETROLERA_ID, MODULO, TIPO_SOLICITUD)).thenReturn(Optional.of(plantilla()));

        assertThatThrownBy(() -> service.crear(PETROLERA_ID, MODULO, TIPO_SOLICITUD, pdf("alta.pdf")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Ya existe una plantilla");

        verify(plantillaDocumentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - Rechaza un archivo que no es PDF aunque diga serlo")
    void crear_ArchivoNoPdf_LanzaBusinessValidation() {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));
        MockMultipartFile falso = new MockMultipartFile("file", "alta.pdf", "application/pdf",
                "PKZIP esto no es un PDF".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.crear(PETROLERA_ID, MODULO, TIPO_SOLICITUD, falso))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("PDF válido");

        verify(plantillaDocumentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - Rechaza un content-type que no es application/pdf")
    void crear_ContentTypeIncorrecto_LanzaBusinessValidation() {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));
        MockMultipartFile imagen = new MockMultipartFile("file", "alta.png", "image/png",
                "%PDF-1.4 disfrazado".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.crear(PETROLERA_ID, MODULO, TIPO_SOLICITUD, imagen))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("debe ser un PDF");

        verify(plantillaDocumentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - Falla si la petrolera no existe")
    void crear_PetroleraInexistente_LanzaResourceNotFound() {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(PETROLERA_ID, MODULO, TIPO_SOLICITUD, pdf("alta.pdf")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Petrolera no encontrada");
    }

    // ---------- descarga por clave (contrato con tarjetas y dispositivos) ----------

    @Test
    @DisplayName("descargarArchivo por clave - Devuelve los bytes del PDF configurado")
    void descargarArchivoPorClave_PlantillaConfigurada_DevuelveBytes() throws Exception {
        Path archivo = storageDir.resolve("plantilla.pdf");
        Files.write(archivo, "%PDF-1.4 alta tarjeta".getBytes(StandardCharsets.UTF_8));

        PlantillaDocumento plantilla = plantilla();
        plantilla.setRutaArchivo(archivo.toString());
        when(plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
                        PETROLERA_ID, MODULO, TIPO_SOLICITUD))
                .thenReturn(Optional.of(plantilla));

        byte[] pdf = service.descargarArchivo(PETROLERA_ID, MODULO, TIPO_SOLICITUD);

        assertThat(new String(pdf, StandardCharsets.UTF_8)).isEqualTo("%PDF-1.4 alta tarjeta");
    }

    @Test
    @DisplayName("descargarArchivo por clave - Sin plantilla configurada lanza 404, no un error genérico")
    void descargarArchivoPorClave_SinPlantilla_LanzaResourceNotFound() {
        when(plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
                        PETROLERA_ID, MODULO, TIPO_SOLICITUD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargarArchivo(PETROLERA_ID, MODULO, TIPO_SOLICITUD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No hay plantilla de documento configurada");
    }

    @Test
    @DisplayName("descargarArchivo por clave - Si el archivo no está en disco lanza 404")
    void descargarArchivoPorClave_ArchivoAusente_LanzaResourceNotFound() {
        PlantillaDocumento plantilla = plantilla();
        plantilla.setRutaArchivo(storageDir.resolve("no-existe.pdf").toString());
        when(plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
                        PETROLERA_ID, MODULO, TIPO_SOLICITUD))
                .thenReturn(Optional.of(plantilla));

        assertThatThrownBy(() -> service.descargarArchivo(PETROLERA_ID, MODULO, TIPO_SOLICITUD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no está disponible");
    }

    // ---------- reemplazo ----------

    @Test
    @DisplayName("reemplazarArchivo - Guarda el nuevo PDF y borra el anterior del disco")
    void reemplazarArchivo_PdfValido_SustituyeArchivo() throws Exception {
        Path anterior = storageDir.resolve("anterior.pdf");
        Files.write(anterior, "%PDF-1.4 anterior".getBytes(StandardCharsets.UTF_8));

        PlantillaDocumento plantilla = plantilla();
        plantilla.setRutaArchivo(anterior.toString());
        when(plantillaDocumentoRepository.findById(PLANTILLA_ID)).thenReturn(Optional.of(plantilla));
        when(plantillaDocumentoRepository.save(any(PlantillaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlantillaDocumentoDTO dto = service.reemplazarArchivo(PLANTILLA_ID, pdf("nueva.pdf"));

        assertThat(anterior).doesNotExist();
        assertThat(Path.of(dto.getRutaArchivo())).exists();
        assertThat(dto.getNombreArchivo()).isEqualTo("nueva.pdf");
    }

    // ---------- borrado lógico ----------

    @Test
    @DisplayName("eliminar - Marca activo y activa a false con deletedAt, sin borrar la fila")
    void eliminar_PlantillaViva_AplicaBorradoLogico() {
        PlantillaDocumento plantilla = plantilla();
        when(plantillaDocumentoRepository.findById(PLANTILLA_ID)).thenReturn(Optional.of(plantilla));
        when(plantillaDocumentoRepository.save(any(PlantillaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.eliminar(PLANTILLA_ID);

        assertThat(plantilla.getActivo()).isFalse();
        assertThat(plantilla.getActiva()).isFalse();
        assertThat(plantilla.getDeletedAt()).isNotNull();
        verify(plantillaDocumentoRepository).save(plantilla);
        verify(plantillaDocumentoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("obtenerPorId - Una plantilla ya eliminada se comporta como inexistente")
    void obtenerPorId_PlantillaEliminada_LanzaResourceNotFound() {
        PlantillaDocumento eliminada = plantilla();
        eliminada.setActivo(false);
        eliminada.setDeletedAt(LocalDateTime.now());
        when(plantillaDocumentoRepository.findById(PLANTILLA_ID)).thenReturn(Optional.of(eliminada));

        assertThatThrownBy(() -> service.obtenerPorId(PLANTILLA_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("eliminar - Tras el borrado lógico la clave vuelve a estar libre")
    void crear_TrasBorradoLogico_PermiteNuevaPlantillaConLaMismaClave() throws Exception {
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));
        // El repositorio solo devuelve plantillas con activo = true: la borrada no bloquea.
        when(plantillaDocumentoRepository.findByPetroleraIdAndModuloAndTipoSolicitudAndActivoTrue(
                PETROLERA_ID, MODULO, TIPO_SOLICITUD)).thenReturn(Optional.empty());
        when(plantillaDocumentoRepository.save(any(PlantillaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlantillaDocumentoDTO dto = service.crear(PETROLERA_ID, MODULO, TIPO_SOLICITUD, pdf("alta.pdf"));

        assertThat(dto.getActiva()).isTrue();
        assertThat(dto.getTipoSolicitud()).isEqualTo(TIPO_SOLICITUD);
    }

    // ---------- estado y listados ----------

    @Test
    @DisplayName("cambiarEstado - Desactiva la plantilla sin borrarla")
    void cambiarEstado_False_DesactivaPeroNoBorra() {
        PlantillaDocumento plantilla = plantilla();
        when(plantillaDocumentoRepository.findById(PLANTILLA_ID)).thenReturn(Optional.of(plantilla));
        when(plantillaDocumentoRepository.save(any(PlantillaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlantillaDocumentoDTO dto = service.cambiarEstado(PLANTILLA_ID, false);

        assertThat(dto.getActiva()).isFalse();
        assertThat(plantilla.getActivo()).isTrue();
        assertThat(plantilla.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("cambiarEstado - Sin estado en el cuerpo lanza error de negocio")
    void cambiarEstado_SinValor_LanzaBusinessValidation() {
        assertThatThrownBy(() -> service.cambiarEstado(PLANTILLA_ID, null))
                .isInstanceOf(BusinessValidationException.class);

        verify(plantillaDocumentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarPorPetrolera - Solo devuelve plantillas no eliminadas")
    void listarPorPetrolera_DevuelveSoloVivas() {
        when(plantillaDocumentoRepository
                .findByPetroleraIdAndActivoTrueOrderByModuloAscTipoSolicitudAsc(PETROLERA_ID))
                .thenReturn(List.of(plantilla()));
        when(petroleraRepository.findById(PETROLERA_ID)).thenReturn(Optional.of(petrolera));

        List<PlantillaDocumentoDTO> plantillas = service.listarPorPetrolera(PETROLERA_ID);

        assertThat(plantillas).hasSize(1);
        assertThat(plantillas.get(0).getModulo()).isEqualTo(MODULO);
        assertThat(plantillas.get(0).getPetroleraNombre()).isEqualTo("Repsol");
        verify(plantillaDocumentoRepository, never()).findAll();
    }

    @Test
    @DisplayName("buscar - Sin plantilla activa lanza 404")
    void buscar_SinPlantillaActiva_LanzaResourceNotFound() {
        when(plantillaDocumentoRepository
                .findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
                        eq(PETROLERA_ID), eq(ModuloDocumento.DISPOSITIVOS), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(PETROLERA_ID, ModuloDocumento.DISPOSITIVOS, "CAMBIO_MATRICULA"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
