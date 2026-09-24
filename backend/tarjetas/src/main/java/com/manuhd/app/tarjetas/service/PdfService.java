package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.exception.BusinessValidationException;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

    /** Nombre de respaldo cuando el que declara el navegador no sirve para mostrarlo. */
    public static final String NOMBRE_DESCONOCIDO = "documento.pdf";

    /** Longitud de la columna que guarda el nombre declarado del fichero. */
    private static final int MAX_NOMBRE = 255;

    /** Cabecera con la que empieza todo PDF ("%PDF-"). */
    private static final byte[] CABECERA_PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};

    @Value("${storage.tarjetas:./storage/tarjetas/solicitudes}")
    private String tarjetasPath;

    /**
     * Nombre declarado por el navegador, dejado en condiciones de guardarse y de mostrarse:
     * solo el nombre del fichero (nunca la ruta con la que venga), sin caracteres de control
     * y recortado a la longitud de la columna.
     *
     * <p>En disco el nombre nunca se usa —cada etapa tiene su fichero de nombre fijo—, pero
     * el valor se persiste y se enseña en la ficha de la solicitud, así que no puede llegar
     * con {@code ../}, con una ruta absoluta ni con un byte nulo dentro.
     */
    public static String nombreOriginalSeguro(MultipartFile file) {
        String declarado = file != null ? file.getOriginalFilename() : null;
        if (declarado == null) {
            return NOMBRE_DESCONOCIDO;
        }

        // Tanto '/' como '\': el nombre puede venir de un cliente de otro sistema operativo
        int ultimoSeparador = Math.max(declarado.lastIndexOf('/'), declarado.lastIndexOf('\\'));
        String nombre = declarado.substring(ultimoSeparador + 1);

        nombre = nombre.replaceAll("\\p{Cntrl}", "").replace("..", "").trim();

        if (nombre.isBlank()) {
            return NOMBRE_DESCONOCIDO;
        }
        return nombre.length() > MAX_NOMBRE ? nombre.substring(0, MAX_NOMBRE) : nombre;
    }

    /**
     * Devuelve el contenido del fichero subido comprobando antes que es un PDF de verdad.
     *
     * <p>Lo que sube la oficina sustituye al impreso descargado de la petrolera o acaba
     * adjunto en el correo que se le manda: un fichero vacío o que solo se llama {@code .pdf}
     * dejaría la solicitud inservible sin que nadie se entere hasta el final del circuito.
     */
    private byte[] contenidoDePdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("El documento está vacío: vuelva a subir el impreso");
        }

        byte[] contenido = file.getBytes();
        exigirCabeceraPdf(contenido, "El documento no es un PDF válido: vuelva a subir el impreso en formato PDF");
        return contenido;
    }

    /** Un PDF empieza siempre por {@code %PDF-}: cualquier otra cosa no se acepta. */
    private void exigirCabeceraPdf(byte[] contenido, String mensajeError) {
        if (contenido == null || contenido.length < CABECERA_PDF.length) {
            throw new BusinessValidationException(mensajeError);
        }
        for (int i = 0; i < CABECERA_PDF.length; i++) {
            if (contenido[i] != CABECERA_PDF[i]) {
                throw new BusinessValidationException(mensajeError);
            }
        }
    }

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
        // Una plantilla vacía dejaría la solicitud recién creada con un impreso de 0 bytes:
        // no se podría aplanar ni mandar al socio, y el fallo no saldría hasta varios pasos
        // después. Mejor no llegar a crearla.
        exigirCabeceraPdf(plantillaPdf, "El impreso recibido de la petrolera no es un PDF válido");

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
        // Se valida ANTES de tocar el disco: el editable que ya hay es la plantilla que mandó
        // la petrolera, y machacarla con basura deja la solicitud sin nada que enviar al socio.
        byte[] contenido = contenidoDePdf(file);

        Path rutaEditable = directorioSolicitud(numeroSolicitud).resolve(EDITABLE);
        Files.write(rutaEditable, contenido);

        log.info("PDF editado guardado en: {}", rutaEditable);
        return rutaEditable.toString();
    }

    public String guardarPdfFirmado(MultipartFile file, String numeroSolicitud) throws IOException {
        byte[] contenido = contenidoDePdf(file);

        Path rutaFirmado = directorioSolicitud(numeroSolicitud).resolve(FIRMADO);
        Files.write(rutaFirmado, contenido);

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

    /**
     * Borra el directorio de una solicitud con todo su contenido.
     *
     * <p>Se usa cuando la creación de la solicitud no llega a cuajar: los impresos se escriben
     * en disco antes de que la transacción confirme, y un directorio huérfano no solo ocupa
     * sitio, sino que la siguiente solicitud reutilizaría ese mismo número y escribiría encima
     * de documentos que no son suyos.
     *
     * <p>Es idempotente: si el directorio no existe no hace nada.
     */
    public void borrarDirectorioSolicitud(String numeroSolicitud) throws IOException {
        if (numeroSolicitud == null || numeroSolicitud.isBlank()) {
            return;
        }

        Path directorio = Paths.get(tarjetasPath, numeroSolicitud);
        if (!Files.isDirectory(directorio)) {
            return;
        }

        // De dentro hacia fuera: un directorio no se puede borrar hasta que está vacío.
        try (Stream<Path> contenido = Files.walk(directorio)) {
            List<Path> deLoMasHondoALoMasSomero = contenido
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path ruta : deLoMasHondoALoMasSomero) {
                Files.deleteIfExists(ruta);
            }
        }

        log.info("Directorio de la solicitud {} borrado: su creación no llegó a completarse", numeroSolicitud);
    }

    public void guardarPdf(byte[] pdfBytes, String rutaDestino) throws IOException {
        Path path = Paths.get(rutaDestino);
        Files.createDirectories(path.getParent());
        Files.write(path, pdfBytes);
        log.info("PDF guardado en: {}", rutaDestino);
    }
}
