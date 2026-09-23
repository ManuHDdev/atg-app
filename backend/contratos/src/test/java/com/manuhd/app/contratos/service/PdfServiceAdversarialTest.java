package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.exception.BusinessValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas adversariales del almacén de PDFs: nombres de fichero hostiles y contenidos que
 * no son PDF.
 *
 * <p>Lo que se comprueba aquí es que el nombre que declara el navegador nunca decide dónde
 * se escribe, y que un fichero vacío o disfrazado no llega a sustituir a un impreso bueno.
 */
@DisplayName("PdfService - nombres y contenidos hostiles")
class PdfServiceAdversarialTest {

    private static final String NUMERO_SOLICITUD = "SOL-2026-00001";

    /** Raíz de trabajo: dentro viven los almacenes, y al lado el "fuera" que nadie debe tocar. */
    @TempDir
    Path raiz;

    private Path plantillas;
    private Path contratos;
    private Path borradores;
    private PdfService pdfService;

    @BeforeEach
    void setUp() throws IOException {
        plantillas = Files.createDirectories(raiz.resolve("storage/plantillas"));
        contratos = Files.createDirectories(raiz.resolve("storage/contratos"));
        borradores = Files.createDirectories(raiz.resolve("storage/borradores"));

        pdfService = new PdfService();
        ReflectionTestUtils.setField(pdfService, "plantillasPath", plantillas.toString());
        ReflectionTestUtils.setField(pdfService, "contratosPath", contratos.toString());
        ReflectionTestUtils.setField(pdfService, "borradoresPath", borradores.toString());
    }

    private byte[] pdfDeUnaPagina() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private MockMultipartFile ficheroPdf(String nombre) throws IOException {
        return new MockMultipartFile("archivo", nombre, "application/pdf", pdfDeUnaPagina());
    }

    private List<Path> ficherosBajoLaRaiz() throws IOException {
        try (var paths = Files.walk(raiz)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }

    // ---------- nombres de fichero hostiles ----------

    /**
     * El nombre lo elige quien sube el fichero. Si se concatena tal cual a la ruta de
     * destino, unos cuantos {@code ../} sacan la escritura del almacén y dejan escribir en
     * cualquier sitio donde alcance el usuario del proceso.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "../../../../evil.pdf",
            "..\\..\\..\\..\\evil.pdf",
            "sub/../../../../evil.pdf",
            "/etc/cron.d/evil.pdf",
            "C:\\Windows\\Temp\\evil.pdf"
    })
    void guardarPlantillaNuncaEscribeFueraDeSuAlmacen(String nombreHostil) throws IOException {
        String ruta = pdfService.guardarPlantilla(ficheroPdf(nombreHostil));

        assertThat(Path.of(ruta).normalize()).startsWith(plantillas);
        assertThat(ficherosBajoLaRaiz())
                .allSatisfy(fichero -> assertThat(fichero.normalize()).startsWith(plantillas));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../../../evil.pdf",
            "..\\..\\..\\..\\evil.pdf",
            "sub/../../../../evil.pdf",
            "/etc/cron.d/evil.pdf"
    })
    void guardarPlantillaOrganizadaNuncaEscribeFueraDeSuAlmacen(String nombreHostil) throws IOException {
        String ruta = pdfService.guardarPlantillaOrganizada(ficheroPdf(nombreHostil), 1L, 2L, 3L);

        assertThat(Path.of(ruta).normalize()).startsWith(plantillas);
        assertThat(ficherosBajoLaRaiz())
                .allSatisfy(fichero -> assertThat(fichero.normalize()).startsWith(plantillas));
    }

    @Test
    void elImpresoFirmadoSiempreCaeEnElDirectorioDeSuSolicitud() throws IOException {
        String ruta = pdfService.guardarPdfFirmado(ficheroPdf("../../../../evil.pdf"), NUMERO_SOLICITUD);

        assertThat(Path.of(ruta).normalize())
                .isEqualTo(contratos.resolve(NUMERO_SOLICITUD).resolve("firmado.pdf"));
    }

    @Test
    void unNombreQueSoloEsUnaRutaNoDejaElFicheroSinNombre() throws IOException {
        String ruta = pdfService.guardarPlantilla(ficheroPdf("../../"));

        assertThat(Path.of(ruta).normalize()).startsWith(plantillas);
        assertThat(Files.isRegularFile(Path.of(ruta))).isTrue();
    }

    // ---------- contenidos que no son PDF ----------

    @Test
    void unImpresoFirmadoVacioNoSeGuarda() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "firmado.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> pdfService.guardarPdfFirmado(vacio, NUMERO_SOLICITUD))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void unEjecutableDisfrazadoDePdfNoSeGuardaComoImpresoFirmado() {
        MockMultipartFile falso = new MockMultipartFile("archivo", "firmado.pdf", "application/pdf",
                "MZ\u0090\u0000no soy un pdf".getBytes(StandardCharsets.ISO_8859_1));

        assertThatThrownBy(() -> pdfService.guardarPdfFirmado(falso, NUMERO_SOLICITUD))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void unImpresoEditadoQueNoEsPdfNoDestruyeElQueYaHabia() throws IOException {
        Path editable = contratos.resolve(NUMERO_SOLICITUD).resolve("editable.pdf");
        Files.createDirectories(editable.getParent());
        byte[] contenidoBueno = pdfDeUnaPagina();
        Files.write(editable, contenidoBueno);

        MockMultipartFile basura = new MockMultipartFile("archivo", "editado.pdf", "application/pdf",
                "esto no es un pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> pdfService.guardarPdfEditado(basura, NUMERO_SOLICITUD))
                .isInstanceOf(BusinessValidationException.class);

        assertThat(Files.readAllBytes(editable)).isEqualTo(contenidoBueno);
    }

    @Test
    void unaPlantillaVaciaNoSeGuarda() {
        MockMultipartFile vacia = new MockMultipartFile("archivo", "plantilla.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> pdfService.guardarPlantilla(vacia))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void unPdfDeVerdadSiSeGuarda() throws IOException {
        String ruta = pdfService.guardarPdfFirmado(ficheroPdf("firmado.pdf"), NUMERO_SOLICITUD);

        assertThat(Files.isRegularFile(Path.of(ruta))).isTrue();
        assertThat(Files.size(Path.of(ruta))).isPositive();
    }
}
