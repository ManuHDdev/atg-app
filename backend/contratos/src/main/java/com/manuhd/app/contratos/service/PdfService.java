package com.manuhd.app.contratos.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PdfService {

    @Value("${storage.plantillas:./storage/plantillas/originales}")
    private String plantillasPath;

    @Value("${storage.contratos:./storage/contratos/solicitudes}")
    private String contratosPath;

    @Value("${storage.borradores:./storage/contratos/borradores}")
    private String borradoresPath;

    public String guardarPlantilla(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(plantillasPath, filename);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toString();
    }

    public List<String> extraerCamposPdf(String rutaPdf) throws IOException {
        List<String> campos = new ArrayList<>();
        File pdfFile = new File(rutaPdf);
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            
            if (acroForm != null) {
                for (PDField field : acroForm.getFields()) {
                    campos.add(field.getFullyQualifiedName());
                }
            }
        }
        
        return campos;
    }

    public String rellenarPdfConDatos(String rutaPlantilla, Map<String, String> datos) throws IOException {
        File plantillaFile = new File(rutaPlantilla);
        String outputFilename = UUID.randomUUID().toString() + ".pdf";
        Path outputPath = Paths.get(borradoresPath, outputFilename);
        Files.createDirectories(outputPath.getParent());
        
        try (PDDocument document = Loader.loadPDF(plantillaFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            
            if (acroForm != null) {
                for (Map.Entry<String, String> entry : datos.entrySet()) {
                    PDField field = acroForm.getField(entry.getKey());
                    if (field != null) {
                        field.setValue(entry.getValue());
                    }
                }
            }
            
            document.save(outputPath.toFile());
        }
        
        return outputPath.toString();
    }

    public String aplanarPdf(String rutaPdf) throws IOException {
        File pdfFile = new File(rutaPdf);
        String outputFilename = UUID.randomUUID().toString() + "_flat.pdf";
        Path outputPath = Paths.get(borradoresPath, outputFilename);
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            
            if (acroForm != null) {
                acroForm.flatten();
            }
            
            document.save(outputPath.toFile());
        }
        
        return outputPath.toString();
    }

    public void copiarPdfADirectorio(String rutaOrigen, String rutaDestino) throws IOException {
        Path origen = Paths.get(rutaOrigen);
        Path destino = Paths.get(rutaDestino);
        Files.createDirectories(destino.getParent());
        Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    public String guardarPlantillaOrganizada(MultipartFile file, Long petroleraId,
                                            Long tipoContratoId, Long tipoSolicitudPetroleraId) throws IOException {
        // Construir ruta organizada: storage/plantillas/originales/{petrolera}/{tipoContrato}/
        StringBuilder pathBuilder = new StringBuilder(plantillasPath);
        pathBuilder.append(File.separator).append("petrolera_").append(petroleraId);
        pathBuilder.append(File.separator).append("tipo_").append(tipoContratoId);

        if (tipoSolicitudPetroleraId != null) {
            pathBuilder.append(File.separator).append("subtipo_").append(tipoSolicitudPetroleraId);
        }

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(pathBuilder.toString(), filename);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Plantilla guardada en: {}", targetPath);
        return targetPath.toString();
    }

    public String copiarPlantillaParaSolicitud(String rutaPlantillaOriginal, String numeroSolicitud) throws IOException {
        // Crear directorio para la solicitud: storage/contratos/solicitudes/{numeroSolicitud}/
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Files.createDirectories(directorioSolicitud);

        // Guardar la plantilla original para poder visualizarla siempre
        Path rutaPlantillaGuardada = directorioSolicitud.resolve("plantilla_original.pdf");
        Files.copy(Paths.get(rutaPlantillaOriginal), rutaPlantillaGuardada, StandardCopyOption.REPLACE_EXISTING);

        // También crear una copia como editable.pdf (esta puede ser sobrescrita por el usuario)
        Path rutaEditable = directorioSolicitud.resolve("editable.pdf");
        Files.copy(Paths.get(rutaPlantillaOriginal), rutaEditable, StandardCopyOption.REPLACE_EXISTING);

        log.info("Plantilla original guardada en: {}", rutaPlantillaGuardada);
        log.info("PDF editable creado en: {}", rutaEditable);
        return rutaEditable.toString();
    }

    public String aplanarPdfParaSolicitud(String rutaPdfEditable, String numeroSolicitud) throws IOException {
        File pdfFile = new File(rutaPdfEditable);
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Path rutaEnviado = directorioSolicitud.resolve("enviado.pdf");

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();

            if (acroForm != null) {
                acroForm.flatten();
            }

            document.save(rutaEnviado.toFile());
        }

        log.info("PDF aplanado guardado en: {}", rutaEnviado);
        return rutaEnviado.toString();
    }

    public String guardarPdfEditado(MultipartFile file, String numeroSolicitud) throws IOException {
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Files.createDirectories(directorioSolicitud);

        Path rutaEditable = directorioSolicitud.resolve("editable.pdf");
        Files.copy(file.getInputStream(), rutaEditable, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF editado guardado en: {}", rutaEditable);
        return rutaEditable.toString();
    }

    public String guardarPdfFirmado(MultipartFile file, String numeroSolicitud) throws IOException {
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Files.createDirectories(directorioSolicitud);

        Path rutaFirmado = directorioSolicitud.resolve("firmado.pdf");
        Files.copy(file.getInputStream(), rutaFirmado, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF firmado guardado en: {}", rutaFirmado);
        return rutaFirmado.toString();
    }

    public String copiarPdfFinal(String rutaPdfFirmado, String numeroSolicitud) throws IOException {
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Path rutaFinal = directorioSolicitud.resolve("final.pdf");

        Files.copy(Paths.get(rutaPdfFirmado), rutaFinal, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF final guardado en: {}", rutaFinal);
        return rutaFinal.toString();
    }

    public byte[] leerPdf(String rutaPdf) throws IOException {
        Path path = Paths.get(rutaPdf);
        if (!Files.exists(path)) {
            throw new IOException("Archivo PDF no encontrado: " + rutaPdf);
        }
        return Files.readAllBytes(path);
    }

    public byte[] leerPlantillaOriginal(String numeroSolicitud) throws IOException {
        Path directorioSolicitud = Paths.get(contratosPath, numeroSolicitud);
        Path rutaPlantilla = directorioSolicitud.resolve("plantilla_original.pdf");

        if (!Files.exists(rutaPlantilla)) {
            throw new IOException("Plantilla original no encontrada para la solicitud: " + numeroSolicitud);
        }

        return Files.readAllBytes(rutaPlantilla);
    }

    public boolean validarPdfEditable(String rutaPdf) throws IOException {
        File pdfFile = new File(rutaPdf);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();

            return acroForm != null && !acroForm.getFields().isEmpty();
        }
    }

    /**
     * Extrae todos los campos del formulario PDF y devuelve información detallada sobre cada uno
     */
    public Map<String, Object> extraerCamposFormulario(String rutaPdf) throws IOException {
        File pdfFile = new File(rutaPdf);
        Map<String, Object> resultado = new java.util.HashMap<>();
        List<Map<String, Object>> camposList = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();

            if (acroForm != null) {
                for (PDField field : acroForm.getFields()) {
                    Map<String, Object> campoInfo = new java.util.HashMap<>();
                    campoInfo.put("nombre", field.getFullyQualifiedName());
                    campoInfo.put("tipo", field.getFieldType());
                    campoInfo.put("valor", field.getValueAsString());

                    // Información adicional según el tipo
                    if ("Tx".equals(field.getFieldType())) { // Text field
                        campoInfo.put("tipoDetallado", "text");
                    } else if ("Btn".equals(field.getFieldType())) { // Button/Checkbox
                        campoInfo.put("tipoDetallado", "checkbox");
                    } else if ("Ch".equals(field.getFieldType())) { // Choice (dropdown/listbox)
                        campoInfo.put("tipoDetallado", "choice");
                        // Obtener opciones si es un campo de selección
                        try {
                            if (field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDChoice) {
                                org.apache.pdfbox.pdmodel.interactive.form.PDChoice choiceField =
                                    (org.apache.pdfbox.pdmodel.interactive.form.PDChoice) field;
                                List<String> opciones = choiceField.getOptions();
                                campoInfo.put("opciones", opciones);
                            }
                        } catch (Exception e) {
                            log.warn("No se pudieron obtener opciones para campo: {}", field.getFullyQualifiedName());
                        }
                    } else {
                        campoInfo.put("tipoDetallado", "unknown");
                    }

                    camposList.add(campoInfo);
                }
            }
        }

        resultado.put("campos", camposList);
        return resultado;
    }

    /**
     * Rellena los campos del formulario PDF con los valores proporcionados
     * @return Array de bytes del PDF modificado
     */
    public byte[] rellenarCamposFormulario(String rutaPdf, Map<String, String> valores) throws IOException {
        File pdfFile = new File(rutaPdf);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();

            if (acroForm != null) {
                for (Map.Entry<String, String> entry : valores.entrySet()) {
                    try {
                        PDField field = acroForm.getField(entry.getKey());
                        if (field != null) {
                            String valor = entry.getValue();
                            if (valor != null && !valor.isEmpty()) {
                                field.setValue(valor);
                                log.debug("Campo '{}' rellenado con valor '{}'", entry.getKey(), valor);
                            }
                        } else {
                            log.warn("Campo no encontrado en PDF: {}", entry.getKey());
                        }
                    } catch (IOException e) {
                        log.error("Error al rellenar campo '{}': {}", entry.getKey(), e.getMessage());
                        // Continuar con los demás campos
                    }
                }
            }

            // Convertir documento a array de bytes
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Guarda un array de bytes como archivo PDF
     */
    public void guardarPdf(byte[] pdfBytes, String rutaDestino) throws IOException {
        Path path = Paths.get(rutaDestino);
        Files.createDirectories(path.getParent());
        Files.write(path, pdfBytes);
        log.info("PDF guardado en: {}", rutaDestino);
    }
}
