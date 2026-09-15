package com.manuhd.app.tarjetas.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Ficheros del circuito del documento firmado. Cada solicitud tiene un directorio propio,
 * nombrado por su número de solicitud, con un fichero por etapa y con nombre fijo:
 * {@code plantilla_original.pdf} (lo que mandó la petrolera, intacto), {@code editable.pdf}
 * (lo que rellena la oficina), {@code enviado.pdf} (aplanado, lo que recibe el socio),
 * {@code firmado.pdf} (lo que devuelve el socio) y {@code final.pdf} (lo que se manda a la
 * petrolera). Nombres fijos porque la etapa, no el nombre del fichero, es lo que identifica
 * a cada documento.
 */
@Service
@Slf4j
public class PdfService {

    public static final String PLANTILLA_ORIGINAL = "plantilla_original.pdf";
    public static final String EDITABLE = "editable.pdf";
    public static final String ENVIADO = "enviado.pdf";
    public static final String FIRMADO = "firmado.pdf";
    public static final String FINAL = "final.pdf";

    @Value("${storage.tarjetas:./storage/tarjetas/solicitudes}")
    private String tarjetasPath;

    /** Directorio de una solicitud, creándolo si todavía no existe. */
    private Path directorioSolicitud(String numeroSolicitud) throws IOException {
        Path directorio = Paths.get(tarjetasPath, numeroSolicitud);
        Files.createDirectories(directorio);
        return directorio;
    }

    /**
     * Guarda la plantilla recién descargada de la petrolera como original inalterable y
     * como punto de partida editable.
     *
     * @return la ruta del PDF editable
     */
    public String copiarPlantillaParaSolicitud(byte[] plantillaPdf, String numeroSolicitud) throws IOException {
        Path directorio = directorioSolicitud(numeroSolicitud);

        Path rutaOriginal = directorio.resolve(PLANTILLA_ORIGINAL);
        Files.write(rutaOriginal, plantillaPdf);

        Path rutaEditable = directorio.resolve(EDITABLE);
        Files.write(rutaEditable, plantillaPdf);

        log.info("Plantilla original guardada en {} y editable en {}", rutaOriginal, rutaEditable);
        return rutaEditable.toString();
    }

    /** Quita los campos de formulario: lo que se manda al socio ya no debe poder editarse. */
    public String aplanarPdfParaSolicitud(String rutaPdfEditable, String numeroSolicitud) throws IOException {
        Path rutaEnviado = directorioSolicitud(numeroSolicitud).resolve(ENVIADO);

        try (PDDocument document = Loader.loadPDF(new File(rutaPdfEditable))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                acroForm.flatten();
            }
            document.save(rutaEnviado.toFile());
        }

        log.info("PDF aplanado guardado en: {}", rutaEnviado);
        return rutaEnviado.toString();
    }

    public String guardarPdfEditado(MultipartFile file, String numeroSolicitud) throws IOException {
        Path rutaEditable = directorioSolicitud(numeroSolicitud).resolve(EDITABLE);
        Files.copy(file.getInputStream(), rutaEditable, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF editado guardado en: {}", rutaEditable);
        return rutaEditable.toString();
    }

    public String guardarPdfFirmado(MultipartFile file, String numeroSolicitud) throws IOException {
        Path rutaFirmado = directorioSolicitud(numeroSolicitud).resolve(FIRMADO);
        Files.copy(file.getInputStream(), rutaFirmado, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF firmado guardado en: {}", rutaFirmado);
        return rutaFirmado.toString();
    }

    /** El documento que se manda a la petrolera es una copia del firmado, congelada en ese momento. */
    public String copiarPdfFinal(String rutaPdfFirmado, String numeroSolicitud) throws IOException {
        Path rutaFinal = directorioSolicitud(numeroSolicitud).resolve(FINAL);
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
        Path rutaPlantilla = Paths.get(tarjetasPath, numeroSolicitud, PLANTILLA_ORIGINAL);
        if (!Files.exists(rutaPlantilla)) {
            throw new IOException("Plantilla original no encontrada para la solicitud: " + numeroSolicitud);
        }
        return Files.readAllBytes(rutaPlantilla);
    }

    public void guardarPdf(byte[] pdfBytes, String rutaDestino) throws IOException {
        Path path = Paths.get(rutaDestino);
        Files.createDirectories(path.getParent());
        Files.write(path, pdfBytes);
        log.info("PDF guardado en: {}", rutaDestino);
    }
}
