package com.manuhd.app.contratos;

import com.manuhd.app.contratos.model.*;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Base class for tests with common test data builders and utilities
 */
public abstract class TestBase {

    protected static final Long TEST_SOCIO_ID = 1L;
    protected static final Long TEST_EMPRESA_ID = 100L;
    protected static final Long TEST_TARJETA_ID = 200L;
    protected static final Long TEST_PETROLERA_ID = 10L;
    protected static final Long TEST_TIPO_CONTRATO_ID = 5L;
    protected static final Long TEST_TIPO_SOLICITUD_PETROLERA_ID = 15L;
    protected static final Long TEST_PLANTILLA_ID = 20L;

    protected Random random = new Random();

    /**
     * Create a test TipoContrato
     */
    protected TipoContrato createTipoContrato(String codigo, String nombre) {
        TipoContrato tipo = new TipoContrato();
        tipo.setCodigo(codigo);
        tipo.setNombre(nombre);
        tipo.setDescripcion("Descripción de prueba para " + nombre);
        tipo.setActivo(true);
        return tipo;
    }

    /**
     * Create a test PlantillaContrato
     */
    protected PlantillaContrato createPlantillaContrato(String nombre, Long petroleraId) {
        PlantillaContrato plantilla = new PlantillaContrato();
        plantilla.setNombrePlantilla(nombre);
        plantilla.setDescripcion("Plantilla de prueba");
        plantilla.setPetroleraId(petroleraId);
        plantilla.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        plantilla.setTipoSolicitudPetroleraId(TEST_TIPO_SOLICITUD_PETROLERA_ID);
        plantilla.setRutaArchivo("/test/plantillas/" + nombre + ".pdf");
        plantilla.setNombreArchivoOriginal(nombre + ".pdf");
        plantilla.setActiva(true);
        plantilla.setVersion(1);
        return plantilla;
    }

    /**
     * Create a test SolicitudContrato
     */
    protected SolicitudContrato createSolicitudContrato(String numeroSolicitud, EstadoSolicitud estado) {
        SolicitudContrato solicitud = new SolicitudContrato();
        solicitud.setNumeroSolicitud(numeroSolicitud);
        solicitud.setSocioId(TEST_SOCIO_ID);
        solicitud.setEmpresaId(TEST_EMPRESA_ID);
        solicitud.setTarjetaId(TEST_TARJETA_ID);
        solicitud.setPetroleraId(TEST_PETROLERA_ID);
        solicitud.setTipoContratoId(TEST_TIPO_CONTRATO_ID);
        solicitud.setTipoSolicitudPetroleraId(TEST_TIPO_SOLICITUD_PETROLERA_ID);
        solicitud.setPlantillaId(TEST_PLANTILLA_ID);
        solicitud.setFechaHoraSolicitud(LocalDateTime.now());
        solicitud.setSolicitadoPor("Test User");
        solicitud.setEsAutonomo(false);
        solicitud.setEstado(estado);
        solicitud.setTipoSolicitud(TipoSolicitudContrato.NUEVO);
        solicitud.setObservaciones("Observaciones de prueba");
        return solicitud;
    }

    /**
     * Create a test ContratoSocio
     */
    protected ContratoSocio createContratoSocio(EstadoContrato estado) {
        ContratoSocio contrato = new ContratoSocio();
        contrato.setSocioId(TEST_SOCIO_ID);
        contrato.setEmpresaId(TEST_EMPRESA_ID);
        contrato.setTarjetaId(TEST_TARJETA_ID);
        contrato.setPetroleraId(TEST_PETROLERA_ID);
        contrato.setSubseccionPetroleraId(1L);
        contrato.setTipoContrato("TIPO_TEST");
        contrato.setTipoSolicitante("EMPRESA");
        contrato.setFechaHoraSolicitud(LocalDateTime.now());
        contrato.setSolicitadoPor("Test User");
        contrato.setEstado(estado);
        contrato.setActivo(true);
        contrato.setFechaVigenciaDesde(LocalDate.now());
        contrato.setFechaVigenciaHasta(LocalDate.now().plusYears(1));
        return contrato;
    }

    /**
     * Create a mock PDF file
     */
    protected MockMultipartFile createMockPdfFile(String filename) {
        byte[] pdfContent = "%PDF-1.4\n%Test PDF content".getBytes();
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                pdfContent
        );
    }

    /**
     * Create a mock PDF file with specific content
     */
    protected MockMultipartFile createMockPdfFileWithContent(String filename, byte[] content) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                content
        );
    }

    /**
     * Generate a random numero solicitud
     */
    protected String generateNumeroSolicitud() {
        int year = LocalDate.now().getYear();
        int number = random.nextInt(99999) + 1;
        return String.format("SOL-%d-%05d", year, number);
    }
}
