package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.client.PetrolerasClient;
import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.EnvioCorreoResult;
import com.manuhd.app.dispositivos.dto.PetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
import com.manuhd.app.dispositivos.exception.BusinessValidationException;
import com.manuhd.app.dispositivos.model.Dispositivo;
import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import com.manuhd.app.dispositivos.repository.DispositivoRepository;
import com.manuhd.app.dispositivos.repository.SolicitudDispositivoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SolicitudDispositivoService {

    @Autowired
    private SolicitudDispositivoRepository solicitudRepository;

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private DispositivoService dispositivoService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private PetrolerasClient petrolerasClient;

    /** Plantillas de correo del circuito del documento firmado, dadas de alta en petroleras. */
    private static final String PLANTILLA_DOCUMENTO_SOCIO = "DOCUMENTO_SOCIO_DISPOSITIVO";
    private static final String PLANTILLA_DOCUMENTO_PETROLERA = "DOCUMENTO_PETROLERA_DISPOSITIVO";

    @Value("${microservices.socios.url:http://localhost:8081}")
    private String sociosBaseUrl;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    @Transactional(readOnly = true)
    public List<SolicitudDispositivoDTO> listarTodas() {
        return solicitudRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SolicitudDispositivoDTO obtenerPorId(Long id) {
        SolicitudDispositivo solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));
        return convertirADTO(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudDispositivoDTO> listarPorSocio(Long socioId) {
        return solicitudRepository.findBySocioId(socioId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudDispositivoDTO> listarPorPetrolera(Long petroleraId) {
        return solicitudRepository.findByPetroleraId(petroleraId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudDispositivoDTO> listarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.findByEstado(estado).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudDispositivoDTO crear(CrearSolicitudDTO dto) {
        log.info("Creando solicitud de dispositivo tipo {} para socio {}", dto.getTipoSolicitud(), dto.getSocioId());

        // Validaciones por tipo
        validarSolicitud(dto);

        SolicitudDispositivo solicitud = new SolicitudDispositivo();
        solicitud.setSocioId(dto.getSocioId());
        solicitud.setEmpresaId(dto.getEmpresaId());
        solicitud.setPetroleraId(dto.getPetroleraId());
        solicitud.setTipoSolicitud(dto.getTipoSolicitud());
        solicitud.setDispositivoId(dto.getDispositivoId());
        solicitud.setEstado(EstadoSolicitud.BORRADOR);
        solicitud.setObservaciones(dto.getObservaciones());
        solicitud.setProgramadoEnvio(dto.getProgramadoEnvio() != null ? dto.getProgramadoEnvio() : false);
        solicitud.setFechaProgramadaEnvio(dto.getFechaProgramadaEnvio());

        switch (dto.getTipoSolicitud()) {
            case ALTA_DISPOSITIVO:
                solicitud.setMatricula(dto.getMatricula().toUpperCase().trim());
                break;
            case SOLICITUD_CREDITO:
                solicitud.setMonto(dto.getMonto());
                // Guardar matricula del dispositivo para referencia
                Dispositivo dispCredito = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                solicitud.setMatricula(dispCredito.getMatricula());
                break;
            case BAJA_DISPOSITIVO:
                Dispositivo dispBaja = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                solicitud.setMatricula(dispBaja.getMatricula());
                break;
            case CAMBIO_MATRICULA:
                Dispositivo dispCambio = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                solicitud.setMatricula(dispCambio.getMatricula());
                solicitud.setMatriculaDestino(dto.getMatriculaDestino().toUpperCase().trim());
                break;
        }

        // La solicitud nace como borrador con el impreso de la petrolera ya descargado: es
        // lo que la oficina cumplimenta y manda al socio para que lo firme.
        String numeroSolicitud = generarNumeroSolicitud();
        solicitud.setNumeroSolicitud(numeroSolicitud);
        solicitud.setRutaPdfEditable(
                descargarPlantillaDeSolicitud(dto.getPetroleraId(), dto.getTipoSolicitud(), numeroSolicitud));
        solicitud.setNombrePdfEditable(PdfService.EDITABLE);

        SolicitudDispositivo guardada = solicitudRepository.save(solicitud);
        log.info("Solicitud de dispositivo creada con ID: {} y numero {}", guardada.getId(), numeroSolicitud);

        // Notificar al socio de que su tramite ha sido registrado
        try {
            enviarNotificacionSocioEtapa(guardada, "NOTIF_SOCIO_DISP_CREADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre creacion: {}", e.getMessage());
        }

        // IMPORTANTE - NO REINTRODUCIR AQUI EL CORREO A LA PETROLERA: la solicitud nace en
        // BORRADOR y todavia no se ha presentado nada. La petrolera se entera en
        // enviarAPetrolera(), que es cuando sale el impreso firmado por el socio. Avisarla
        // tambien al crear duplicaba el envio y anunciaba algo que aun no existia para ella.

        return convertirADTO(guardada);
    }

    private void validarSolicitud(CrearSolicitudDTO dto) {
        validarRestriccionesPetrolera(dto);

        switch (dto.getTipoSolicitud()) {
            case ALTA_DISPOSITIVO:
                if (dto.getMatricula() == null || dto.getMatricula().trim().isEmpty()) {
                    throw new RuntimeException("La matricula es obligatoria para alta de dispositivo");
                }
                if (!dispositivoService.validarMatriculaDisponible(dto.getMatricula().trim())) {
                    throw new RuntimeException("Ya existe un dispositivo activo con la matricula: " + dto.getMatricula());
                }
                break;

            case SOLICITUD_CREDITO:
                if (dto.getDispositivoId() == null) {
                    throw new RuntimeException("Debe seleccionar un dispositivo para solicitar credito");
                }
                Dispositivo dispCredito = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                if (!dispCredito.getActivo()) {
                    throw new RuntimeException("El dispositivo seleccionado no esta activo");
                }
                if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("El monto debe ser mayor que 0 para solicitud de credito");
                }
                break;

            case BAJA_DISPOSITIVO:
                if (dto.getDispositivoId() == null) {
                    throw new RuntimeException("Debe seleccionar un dispositivo para dar de baja");
                }
                Dispositivo dispBaja = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                if (!dispBaja.getActivo()) {
                    throw new RuntimeException("El dispositivo seleccionado ya esta dado de baja");
                }
                break;

            case CAMBIO_MATRICULA:
                if (dto.getDispositivoId() == null) {
                    throw new RuntimeException("Debe seleccionar un dispositivo para cambio de matricula");
                }
                Dispositivo dispCambio = dispositivoRepository.findById(dto.getDispositivoId())
                        .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
                if (!dispCambio.getActivo()) {
                    throw new RuntimeException("El dispositivo seleccionado no esta activo");
                }
                if (dto.getMatriculaDestino() == null || dto.getMatriculaDestino().trim().isEmpty()) {
                    throw new RuntimeException("La matricula destino es obligatoria para cambio de matricula");
                }
                if (!dispositivoService.validarMatriculaDisponible(dto.getMatriculaDestino().trim())) {
                    throw new RuntimeException("Ya existe un dispositivo activo con la matricula destino: " + dto.getMatriculaDestino());
                }
                break;
        }
    }

    // ---------- circuito del documento firmado ----------

    /**
     * Sustituye el impreso editable por el que ha rellenado la oficina. Solo tiene sentido
     * mientras la solicitud sigue en BORRADOR: despues el documento ya esta en manos del socio.
     */
    @Transactional
    public SolicitudDispositivoDTO guardarPdfEditado(Long solicitudId, MultipartFile pdfEditado) throws IOException {
        log.info("Guardando PDF editado de la solicitud: {}", solicitudId);

        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new BusinessValidationException("Solo se puede editar el impreso de una solicitud en borrador");
        }

        solicitud.setRutaPdfEditable(pdfService.guardarPdfEditado(pdfEditado, exigirNumeroSolicitud(solicitud)));
        solicitud.setNombrePdfEditable(PdfService.nombreOriginalSeguro(pdfEditado));

        return convertirADTO(solicitudRepository.save(solicitud));
    }

    /**
     * Manda al socio el impreso aplanado para que lo firme. El socio firma fuera del sistema
     * y devuelve el documento por el canal que prefiera; aqui solo se deja constancia del envio.
     */
    @Transactional
    public SolicitudDispositivoDTO enviarASocio(Long solicitudId) throws IOException {
        log.info("Enviando a socio el impreso de la solicitud: {}", solicitudId);

        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new BusinessValidationException("Solo se puede enviar al socio una solicitud en borrador");
        }

        String numeroSolicitud = exigirNumeroSolicitud(solicitud);
        String mensajeFaltaPdf = "No se puede enviar al socio: falta el impreso de la solicitud " + numeroSolicitud;

        // Sin impreso no hay nada que firmar: se comprueba ANTES de dar el envio por hecho.
        validarPdfDisponible(solicitud.getRutaPdfEditable(), mensajeFaltaPdf);

        String rutaPdfEnviado = pdfService.aplanarPdfParaSolicitud(solicitud.getRutaPdfEditable(), numeroSolicitud);
        Path adjunto = validarPdfDisponible(rutaPdfEnviado, mensajeFaltaPdf);

        solicitud.setRutaPdfEnviado(rutaPdfEnviado);
        solicitud.setNombrePdfEnviado(PdfService.ENVIADO);
        solicitud.setEstado(EstadoSolicitud.ENVIADO_SOCIO);
        solicitud.setFechaEnvioSocio(LocalDateTime.now());

        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);

        // Un fallo de correo no deshace el envio: el documento ya esta aplanado y la etapa
        // avanzada. El resultado queda registrado en el historial de correos.
        String emailSocio = null;
        try {
            Map<String, Object> socio = obtenerDatosSocio(actualizada.getSocioId());
            Map<String, Object> petrolera = obtenerDatosPetrolera(actualizada.getPetroleraId());
            Map<String, Object> empresa = actualizada.getEmpresaId() != null
                    ? obtenerDatosEmpresa(actualizada.getEmpresaId()) : null;
            emailSocio = emailDestino((String) socio.get("email"), "socio@example.com");

            Map<String, Object> plantilla = obtenerPlantillaCorreo(
                    actualizada.getPetroleraId(), PLANTILLA_DOCUMENTO_SOCIO);
            if (plantilla == null) {
                // Sin plantilla configurada no se manda nada, pero la etapa ya ha avanzado.
                log.warn("No hay plantilla {} para la petrolera {}; no se envia el impreso al socio",
                        PLANTILLA_DOCUMENTO_SOCIO, actualizada.getPetroleraId());
            } else {
                Map<String, String> variables = prepararVariablesCorreo(actualizada, socio, empresa, petrolera);
                emailService.enviarCorreoConPlantillaYAdjunto(
                        emailSocio,
                        (String) plantilla.get("asunto"),
                        (String) plantilla.get("cuerpo"),
                        variables,
                        adjunto,
                        nombreAdjunto("Solicitud", actualizada));
                registrarEnvioCorreo(actualizada, PLANTILLA_DOCUMENTO_SOCIO, emailSocio, true, null);
            }
        } catch (Exception e) {
            log.error("Error al enviar el impreso al socio: {}", e.getMessage());
            registrarEnvioCorreo(actualizada, PLANTILLA_DOCUMENTO_SOCIO,
                    emailSocio != null ? emailSocio : "socio", false, e.getMessage());
        }

        return convertirADTO(actualizada);
    }

    /**
     * Guarda el escaneado que ha devuelto el socio. No cambia el estado a proposito: el
     * escaneado puede venir torcido o incompleto y se sustituye tantas veces como haga falta;
     * el paso lo cierra explicitamente {@link #aceptarFirmaSocio(Long)}.
     */
    @Transactional
    public SolicitudDispositivoDTO subirPdfFirmado(Long solicitudId, MultipartFile pdfFirmado) throws IOException {
        log.info("Subiendo el impreso firmado de la solicitud: {}", solicitudId);

        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new BusinessValidationException(
                    "Solo se puede subir el impreso firmado de una solicitud enviada al socio");
        }

        solicitud.setRutaPdfFirmado(pdfService.guardarPdfFirmado(pdfFirmado, exigirNumeroSolicitud(solicitud)));
        solicitud.setNombrePdfFirmado(PdfService.nombreOriginalSeguro(pdfFirmado));
        solicitud.setFechaRecepcionFirmado(LocalDateTime.now());

        return convertirADTO(solicitudRepository.save(solicitud));
    }

    /** Da por buena la firma recibida y deja la solicitud lista para presentarla a la petrolera. */
    @Transactional
    public SolicitudDispositivoDTO aceptarFirmaSocio(Long solicitudId) {
        log.info("Aceptando la firma del socio en la solicitud: {}", solicitudId);

        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new BusinessValidationException(
                    "Solo se puede aceptar la firma de una solicitud enviada al socio");
        }

        validarPdfDisponible(solicitud.getRutaPdfFirmado(),
                "Debe subir el impreso firmado antes de aceptar la firma");

        solicitud.setEstado(EstadoSolicitud.FIRMADO_SOCIO);

        return convertirADTO(solicitudRepository.save(solicitud));
    }

    /**
     * Presenta la solicitud a la petrolera: un correo por solicitud, con el impreso firmado
     * adjunto y los datos del socio en el cuerpo. Nunca se agrupan varias en un mismo envio.
     */
    @Transactional
    public SolicitudDispositivoDTO enviarAPetrolera(Long solicitudId) throws IOException {
        log.info("Enviando a petrolera la solicitud: {}", solicitudId);

        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        // Las filas creadas antes del circuito del documento firmado quedaron en PENDIENTE y
        // no tienen numero de solicitud, asi que no pueden entrar en el circuito ni llegar
        // nunca a FIRMADO_SOCIO. Para no dejarlas bloqueadas en produccion se siguen enviando
        // como antes: correo a la petrolera sin impreso adjunto.
        if (esSolicitudHeredada(solicitud)) {
            return enviarAPetroleraSinDocumento(solicitud);
        }

        if (solicitud.getEstado() != EstadoSolicitud.FIRMADO_SOCIO) {
            throw new BusinessValidationException(
                    "Solo se puede enviar a la petrolera una solicitud con la firma del socio aceptada");
        }

        String numeroSolicitud = exigirNumeroSolicitud(solicitud);
        String mensajeFaltaPdf = "No se puede enviar a la petrolera: falta el impreso firmado de la solicitud "
                + numeroSolicitud;

        validarPdfDisponible(solicitud.getRutaPdfFirmado(), mensajeFaltaPdf);

        String rutaPdfFinal = pdfService.copiarPdfFinal(solicitud.getRutaPdfFirmado(), numeroSolicitud);
        Path adjunto = validarPdfDisponible(rutaPdfFinal, mensajeFaltaPdf);

        solicitud.setRutaPdfFinal(rutaPdfFinal);
        solicitud.setNombrePdfFinal(PdfService.FINAL);
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());

        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);

        String emailPetrolera = null;
        try {
            Map<String, Object> socio = obtenerDatosSocio(actualizada.getSocioId());
            Map<String, Object> petrolera = obtenerDatosPetrolera(actualizada.getPetroleraId());
            Map<String, Object> empresa = actualizada.getEmpresaId() != null
                    ? obtenerDatosEmpresa(actualizada.getEmpresaId()) : null;
            emailPetrolera = emailDestino((String) petrolera.get("email"), "admin@atg.com");

            Map<String, String> variables = prepararVariablesCorreo(actualizada, socio, empresa, petrolera);

            // Este correo no puede dejar de salir por una plantilla sin configurar: es el
            // que presenta la solicitud a la petrolera.
            Map<String, Object> plantilla = obtenerPlantillaCorreo(
                    actualizada.getPetroleraId(), PLANTILLA_DOCUMENTO_PETROLERA);
            String asunto = plantilla != null
                    ? (String) plantilla.get("asunto") : asuntoPetroleraPorDefecto(actualizada);
            String cuerpo = plantilla != null
                    ? (String) plantilla.get("cuerpo") : cuerpoPetroleraPorDefecto(actualizada);

            emailService.enviarCorreoConPlantillaYAdjunto(emailPetrolera, asunto, cuerpo, variables,
                    adjunto, nombreAdjunto("Solicitud-firmada", actualizada));
            registrarEnvioCorreo(actualizada, PLANTILLA_DOCUMENTO_PETROLERA, emailPetrolera, true, null);
        } catch (Exception e) {
            log.error("Error al presentar la solicitud a la petrolera: {}", e.getMessage());
            registrarEnvioCorreo(actualizada, PLANTILLA_DOCUMENTO_PETROLERA,
                    emailPetrolera != null ? emailPetrolera : "petrolera", false, e.getMessage());
        }

        // Notificar al socio de que su solicitud ya esta en manos de la petrolera
        try {
            enviarNotificacionSocioEtapa(actualizada, "NOTIF_SOCIO_DISP_ENVIADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre envio: {}", e.getMessage());
        }

        return convertirADTO(actualizada);
    }

    /** Devuelve el PDF de una etapa concreta del circuito. */
    @Transactional(readOnly = true)
    public byte[] descargarPdf(Long solicitudId, TipoPdf tipo) throws IOException {
        SolicitudDispositivo solicitud = obtenerSolicitud(solicitudId);

        String rutaPdf = switch (tipo) {
            case EDITABLE -> solicitud.getRutaPdfEditable();
            case ENVIADO -> solicitud.getRutaPdfEnviado();
            case FIRMADO -> solicitud.getRutaPdfFirmado();
            case FINAL -> solicitud.getRutaPdfFinal();
        };

        if (rutaPdf == null) {
            throw new BusinessValidationException("El PDF " + tipo + " no esta disponible para esta solicitud");
        }

        return pdfService.leerPdf(rutaPdf);
    }

    /** Etapas del circuito que tienen un PDF descargable. */
    public enum TipoPdf {
        EDITABLE, ENVIADO, FIRMADO, FINAL
    }

    /**
     * Una solicitud anterior al circuito del documento firmado: se quedo en el estado
     * heredado PENDIENTE y nunca tuvo numero de solicitud ni impreso asociado.
     */
    private boolean esSolicitudHeredada(SolicitudDispositivo solicitud) {
        return solicitud.getEstado() == EstadoSolicitud.PENDIENTE
                && (solicitud.getNumeroSolicitud() == null || solicitud.getNumeroSolicitud().isBlank());
    }

    /**
     * Presentacion a la petrolera de una solicitud heredada: el mismo correo sin adjunto que
     * se enviaba antes del circuito del documento firmado. No se usa nunca con solicitudes
     * nuevas, que siempre llevan su impreso firmado.
     */
    private SolicitudDispositivoDTO enviarAPetroleraSinDocumento(SolicitudDispositivo solicitud) {
        log.info("Solicitud heredada {}: se presenta a la petrolera sin impreso adjunto", solicitud.getId());

        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());
        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);

        String emailPetrolera = null;
        try {
            Map<String, Object> socio = obtenerDatosSocio(actualizada.getSocioId());
            Map<String, Object> petrolera = obtenerDatosPetrolera(actualizada.getPetroleraId());
            Map<String, Object> empresa = actualizada.getEmpresaId() != null
                    ? obtenerDatosEmpresa(actualizada.getEmpresaId()) : null;
            emailPetrolera = emailDestino((String) petrolera.get("email"), "admin@atg.com");

            Map<String, String> variables = prepararVariablesCorreo(actualizada, socio, empresa, petrolera);
            Map<String, Object> plantilla = obtenerPlantillaCorreo(
                    actualizada.getPetroleraId(), PLANTILLA_DOCUMENTO_PETROLERA);
            String asunto = plantilla != null
                    ? (String) plantilla.get("asunto") : asuntoPetroleraPorDefecto(actualizada);
            String cuerpo = plantilla != null
                    ? (String) plantilla.get("cuerpo") : cuerpoPetroleraPorDefecto(actualizada);

            emailService.enviarCorreoConPlantilla(emailPetrolera, asunto, cuerpo, variables);
            registrarEnvioCorreo(actualizada, "ENVIO_PETROLERA", emailPetrolera, true, null);
        } catch (Exception e) {
            log.error("Error enviando correo a petrolera: {}", e.getMessage());
            registrarEnvioCorreo(actualizada, "ENVIO_PETROLERA",
                    emailPetrolera != null ? emailPetrolera : "petrolera", false, e.getMessage());
        }

        try {
            enviarNotificacionSocioEtapa(actualizada, "NOTIF_SOCIO_DISP_ENVIADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre envio: {}", e.getMessage());
        }

        return convertirADTO(actualizada);
    }

    @Transactional
    public SolicitudDispositivoDTO responderPetrolera(Long solicitudId, boolean aprobado, String respuesta,
            BigDecimal montoConcedido) {
        SolicitudDispositivo solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("La solicitud no esta en estado ENVIADO_PETROLERA");
        }

        // El importe concedido solo aplica a la solicitud de credito y solo al aprobar.
        // Al denegar (o en cualquier otro tipo) se ignora lo que llegue y se deja a null.
        if (aprobado) {
            solicitud.setMontoConcedido(validarMontoConcedido(solicitud, montoConcedido));
        } else {
            solicitud.setMontoConcedido(null);
        }

        solicitud.setEstado(aprobado ? EstadoSolicitud.APROBADO : EstadoSolicitud.DENEGADO);
        solicitud.setRespuestaPetrolera(respuesta);
        solicitud.setFechaRespuestaPetrolera(LocalDateTime.now());

        // El motivo del rechazo se guarda ademas en su propio campo: respuestaPetrolera es un
        // cajon compartido que cualquier paso posterior puede sobrescribir.
        solicitud.setMotivoRechazo(aprobado ? null : respuesta);

        // Si aprobado, ejecutar accion segun tipo
        if (aprobado) {
            ejecutarAccionAprobacion(solicitud);
        }

        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);
        log.info("Solicitud {} respondida: {}", solicitudId, aprobado ? "APROBADO" : "DENEGADO");

        // Notificar al socio automaticamente
        notificarSocio(solicitudId);

        return convertirADTO(actualizada);
    }

    /**
     * Valida el importe concedido al aprobar una solicitud.
     *
     * Solo la solicitud de credito lleva importe; el resto de tipos se guardan siempre a null.
     */
    private BigDecimal validarMontoConcedido(SolicitudDispositivo solicitud, BigDecimal montoConcedido) {
        if (solicitud.getTipoSolicitud() != TipoSolicitud.SOLICITUD_CREDITO) {
            return null;
        }
        if (montoConcedido == null || montoConcedido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El importe concedido es obligatorio y debe ser mayor que 0");
        }
        return montoConcedido;
    }

    private void ejecutarAccionAprobacion(SolicitudDispositivo solicitud) {
        switch (solicitud.getTipoSolicitud()) {
            case ALTA_DISPOSITIVO:
                Dispositivo nuevo = dispositivoService.crearDispositivo(
                    solicitud.getSocioId(),
                    solicitud.getPetroleraId(),
                    solicitud.getMatricula(),
                    solicitud.getId()
                );
                solicitud.setDispositivoId(nuevo.getId());
                log.info("Dispositivo creado con ID {} para solicitud {}", nuevo.getId(), solicitud.getId());
                break;

            case BAJA_DISPOSITIVO:
                dispositivoService.darDeBaja(solicitud.getDispositivoId());
                log.info("Dispositivo {} dado de baja", solicitud.getDispositivoId());
                break;

            case CAMBIO_MATRICULA:
                dispositivoService.cambiarMatricula(solicitud.getDispositivoId(), solicitud.getMatriculaDestino());
                log.info("Matricula del dispositivo {} cambiada a {}", solicitud.getDispositivoId(), solicitud.getMatriculaDestino());
                break;

            case SOLICITUD_CREDITO:
                // Para credito no hay accion adicional sobre el dispositivo
                log.info("Credito aprobado para dispositivo {}", solicitud.getDispositivoId());
                break;
        }
    }

    @Transactional
    public SolicitudDispositivoDTO notificarSocio(Long solicitudId) {
        SolicitudDispositivo solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.APROBADO && solicitud.getEstado() != EstadoSolicitud.DENEGADO) {
            throw new RuntimeException("La solicitud no tiene respuesta de la petrolera");
        }

        Map<String, Object> socio = obtenerDatosSocio(solicitud.getSocioId());
        Map<String, Object> petrolera = obtenerDatosPetrolera(solicitud.getPetroleraId());
        String emailSocio = (String) socio.get("email");

        if (emailSocio == null || emailSocio.isEmpty()) {
            emailSocio = "socio@example.com";
        }

        String tipoLabel = getTipoSolicitudLabel(solicitud.getTipoSolicitud());
        String asunto = String.format("Respuesta sobre su solicitud de %s", tipoLabel);

        String cuerpo = String.format(
            "<html><body>" +
            "<h2>Estimado/a %s</h2>" +
            "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido <strong>%s</strong> por la petrolera <strong>%s</strong>.</p>" +
            "<p><strong>Detalles:</strong></p>" +
            "<ul>" +
            "<li>Tipo: %s</li>" +
            "<li>Matricula: %s</li>" +
            "%s" +
            "%s" +
            "<li>Estado: %s</li>" +
            "<li>Respuesta: %s</li>" +
            "</ul>" +
            "<p>Para mas informacion, contacte con nosotros.</p>" +
            "<p>Saludos cordiales,<br>Equipo ATG</p>" +
            "</body></html>",
            socio.get("nombre"),
            tipoLabel,
            solicitud.getEstado() == EstadoSolicitud.APROBADO ? "APROBADA" : "DENEGADA",
            petrolera.get("nombre"),
            tipoLabel,
            solicitud.getMatricula() != null ? solicitud.getMatricula() : "N/A",
            solicitud.getMonto() != null ? "<li>Importe solicitado: " + solicitud.getMonto().toString() + " &euro;</li>" : "",
            solicitud.getMontoConcedido() != null
                ? "<li>Importe concedido: " + solicitud.getMontoConcedido().toString() + " &euro;</li>"
                : "",
            solicitud.getEstado(),
            solicitud.getRespuestaPetrolera() != null ? solicitud.getRespuestaPetrolera() : "Sin comentarios"
        );

        try {
            emailService.enviarCorreoHTML(emailSocio, asunto, cuerpo);
            registrarEnvioCorreo(solicitud, "NOTIF_SOCIO_DISP_RESULTADO", emailSocio, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(solicitud, "NOTIF_SOCIO_DISP_RESULTADO", emailSocio, false, e.getMessage());
            log.error("Error notificando socio resultado: {}", e.getMessage());
        }

        solicitud.setEstado(EstadoSolicitud.COMPLETADO);
        solicitud.setFechaNotificacionSocio(LocalDateTime.now());
        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);

        log.info("Socio notificado sobre solicitud {}", solicitudId);

        return convertirADTO(actualizada);
    }

    // --- Helpers ---

    private SolicitudDispositivo obtenerSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));
    }

    /**
     * El numero de solicitud da nombre al directorio de sus PDFs. Las solicitudes creadas
     * antes del circuito de firma no lo tienen, asi que no pueden entrar en el.
     */
    private String exigirNumeroSolicitud(SolicitudDispositivo solicitud) {
        String numeroSolicitud = solicitud.getNumeroSolicitud();
        if (numeroSolicitud == null || numeroSolicitud.isBlank()) {
            throw new BusinessValidationException("La solicitud no tiene numero asignado: se creo antes del "
                    + "circuito de firma y su documentacion debe tramitarse fuera del sistema");
        }
        return numeroSolicitud;
    }

    private String generarNumeroSolicitud() {
        String prefijo = "DIS-" + Year.now().getValue() + "-";
        Integer maxNumero = solicitudRepository.findMaxNumeroSolicitudByYear(prefijo);
        return String.format("%s%05d", prefijo, (maxNumero != null ? maxNumero : 0) + 1);
    }

    /**
     * Descarga de la petrolera el impreso del tipo de solicitud y lo deja en el directorio
     * de la solicitud. Sin impreso no hay nada que firmar, asi que la creacion se detiene.
     *
     * @return la ruta del PDF editable recien creado
     */
    private String descargarPlantillaDeSolicitud(Long petroleraId, TipoSolicitud tipo, String numeroSolicitud) {
        try {
            byte[] plantillaPdf = petrolerasClient.obtenerPlantillaDocumento(petroleraId, tipo);
            return pdfService.copiarPlantillaParaSolicitud(plantillaPdf, numeroSolicitud);
        } catch (IOException e) {
            log.error("Error al obtener la plantilla de {} para la petrolera {}", tipo, petroleraId, e);
            if (e.getMessage() != null && e.getMessage().startsWith("PLANTILLA_NO_CONFIGURADA:")) {
                throw new BusinessValidationException(
                        e.getMessage().substring("PLANTILLA_NO_CONFIGURADA: ".length()));
            }
            throw new BusinessValidationException(
                    "No se pudo obtener el impreso de la petrolera. Intentelo de nuevo en unos momentos.");
        }
    }

    /**
     * Comprueba que el PDF que se va a adjuntar existe y es legible antes de dar el envio
     * por hecho, para que ninguna etapa avance con un documento que no se puede mandar.
     *
     * @return la ruta validada, lista para adjuntar
     */
    private Path validarPdfDisponible(String rutaPdf, String mensajeError) {
        if (rutaPdf == null || rutaPdf.isBlank()) {
            throw new BusinessValidationException(mensajeError);
        }
        Path ruta = Paths.get(rutaPdf);
        if (!Files.isRegularFile(ruta) || !Files.isReadable(ruta)) {
            throw new BusinessValidationException(mensajeError);
        }
        return ruta;
    }

    private String emailDestino(String email, String porDefecto) {
        return (email == null || email.isEmpty()) ? porDefecto : email;
    }

    private String nombreAdjunto(String prefijo, SolicitudDispositivo solicitud) {
        return prefijo + "-" + solicitud.getNumeroSolicitud() + ".pdf";
    }

    private String asuntoPetroleraPorDefecto(SolicitudDispositivo solicitud) {
        return "Solicitud de dispositivo - " + getTipoSolicitudLabel(solicitud.getTipoSolicitud())
                + " - " + solicitud.getNumeroSolicitud();
    }

    /**
     * Cuerpo de respaldo del correo a la petrolera, con los datos del socio que la petrolera
     * necesita para tramitar la solicitud. Las variables las resuelve despues el EmailService.
     */
    private String cuerpoPetroleraPorDefecto(SolicitudDispositivo solicitud) {
        return "<html><body>"
                + "<h2>Solicitud de dispositivo - " + getTipoSolicitudLabel(solicitud.getTipoSolicitud()) + "</h2>"
                + "<p>Estimados,</p>"
                + "<p>Adjuntamos la solicitud firmada por el socio con los siguientes datos:</p>"
                + "<table style='border-collapse: collapse; margin: 20px 0;'>"
                + filaPetrolera("N&ordm; Solicitud", solicitud.getNumeroSolicitud())
                + filaPetrolera("Socio", "{{socio_nombre}}")
                + filaPetrolera("NIF", "{{socio_nif}}")
                + filaPetrolera("N&ordm; de socio", "{{socio_numero}}")
                + filaPetrolera("Correo", "{{socio_email}}")
                + filaPetrolera("Tipo de solicitud", "{{tipo_solicitud}}")
                + filaPetrolera("Matr&iacute;cula", "{{matricula}}")
                + filaPetrolera("Matr&iacute;cula destino", "{{matricula_destino}}")
                + filaPetrolera("Importe solicitado", "{{monto}}")
                + filaPetrolera("Observaciones", "{{observaciones}}")
                + "</table>"
                + "<p>Saludos cordiales,<br/>Sistema de Gesti&oacute;n ATG</p>"
                + "</body></html>";
    }

    private String filaPetrolera(String etiqueta, String valor) {
        return "<tr><td style='padding: 8px; font-weight: bold;'>" + etiqueta + ":</td>"
                + "<td style='padding: 8px;'>" + (valor != null ? valor : "") + "</td></tr>";
    }

    private String getTipoSolicitudLabel(TipoSolicitud tipo) {
        switch (tipo) {
            case ALTA_DISPOSITIVO: return "Alta de Dispositivo";
            case SOLICITUD_CREDITO: return "Credito de Dispositivo";
            case BAJA_DISPOSITIVO: return "Baja de Dispositivo";
            case CAMBIO_MATRICULA: return "Cambio de Matricula";
            default: return tipo.name();
        }
    }

    private Map<String, String> prepararVariablesCorreo(SolicitudDispositivo solicitud,
            Map<String, Object> socio, Map<String, Object> empresa, Map<String, Object> petrolera) {
        Map<String, String> variables = new HashMap<>();

        variables.put("socio_nombre", socio.get("nombre") != null ? (String) socio.get("nombre") : "");
        variables.put("socio_email", socio.get("email") != null ? (String) socio.get("email") : "");
        variables.put("socio_nif", socio.get("nif") != null ? (String) socio.get("nif") : "");
        variables.put("socio_numero", socio.get("numeroSocio") != null ? (String) socio.get("numeroSocio") : "");

        if (empresa != null) {
            variables.put("empresa_nombre", empresa.get("nombre") != null ? (String) empresa.get("nombre") : "");
            variables.put("empresa_cif", empresa.get("cif") != null ? (String) empresa.get("cif") : "");
        }

        variables.put("petrolera_nombre", petrolera.get("nombre") != null ? (String) petrolera.get("nombre") : "");
        variables.put("tipo_solicitud", getTipoSolicitudLabel(solicitud.getTipoSolicitud()));
        variables.put("matricula", solicitud.getMatricula() != null ? solicitud.getMatricula() : "");
        variables.put("matricula_destino", solicitud.getMatriculaDestino() != null ? solicitud.getMatriculaDestino() : "");
        variables.put("monto", solicitud.getMonto() != null ? solicitud.getMonto().toString() : "N/A");
        variables.put("monto_concedido",
                solicitud.getMontoConcedido() != null ? solicitud.getMontoConcedido().toString() : "N/A");
        variables.put("observaciones", solicitud.getObservaciones() != null ? solicitud.getObservaciones() : "");

        return variables;
    }

    private void registrarEnvioCorreo(SolicitudDispositivo solicitud, String tipoPlantilla, String destinatario, boolean exito, String errorMsg) {
        EnvioCorreoResult resultado = exito
                ? new EnvioCorreoResult(true, tipoPlantilla, destinatario)
                : new EnvioCorreoResult(false, tipoPlantilla, destinatario, errorMsg);

        StringBuilder sb = new StringBuilder(
                solicitud.getCorreosEnviados() != null ? solicitud.getCorreosEnviados() : ""
        );
        if (sb.length() > 0) sb.append("\n");
        sb.append(resultado.toString());
        solicitud.setCorreosEnviados(sb.toString());
        solicitudRepository.save(solicitud);
    }

    private void enviarNotificacionSocioEtapa(SolicitudDispositivo solicitud, String tipoNotificacion) {
        Map<String, Object> socio = obtenerDatosSocio(solicitud.getSocioId());
        Map<String, Object> petrolera = obtenerDatosPetrolera(solicitud.getPetroleraId());
        String emailSocio = (String) socio.get("email");
        if (emailSocio == null || emailSocio.isEmpty()) {
            emailSocio = "socio@example.com";
        }

        String nombreSocio = (String) socio.get("nombre");
        String nombrePetrolera = (String) petrolera.get("nombre");
        String tipoLabel = getTipoSolicitudLabel(solicitud.getTipoSolicitud());

        // Intentar obtener plantilla personalizada
        String tipoPlantillaBackend = tipoNotificacion;
        Map<String, Object> plantillaCorreo = obtenerPlantillaCorreo(solicitud.getPetroleraId(), tipoPlantillaBackend);

        String asunto;
        String cuerpo;

        if (plantillaCorreo != null) {
            Map<String, String> variables = new HashMap<>();
            variables.put("socio_nombre", nombreSocio != null ? nombreSocio : "");
            variables.put("socio_email", emailSocio);
            variables.put("petrolera_nombre", nombrePetrolera != null ? nombrePetrolera : "");
            variables.put("tipo_solicitud", tipoLabel);
            variables.put("matricula", solicitud.getMatricula() != null ? solicitud.getMatricula() : "");

            asunto = emailService.procesarPlantilla((String) plantillaCorreo.get("asunto"), variables);
            cuerpo = emailService.procesarPlantilla((String) plantillaCorreo.get("cuerpo"), variables);
        } else {
            switch (tipoNotificacion) {
                case "NOTIF_SOCIO_DISP_CREADO":
                    asunto = "Su solicitud de " + tipoLabel + " ha sido registrada";
                    cuerpo = String.format(
                        "<html><body>" +
                        "<h2>Estimado/a %s</h2>" +
                        "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido registrada correctamente.</p>" +
                        "<p>Matricula: <strong>%s</strong></p>" +
                        "<p>Procederemos a enviarla a <strong>%s</strong> para su tramitacion.</p>" +
                        "<p>Le mantendremos informado/a del estado de su solicitud.</p>" +
                        "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                        "</body></html>",
                        nombreSocio, tipoLabel,
                        solicitud.getMatricula() != null ? solicitud.getMatricula() : "N/A",
                        nombrePetrolera);
                    break;
                case "NOTIF_SOCIO_DISP_ENVIADO":
                    asunto = "Su solicitud de " + tipoLabel + " ha sido enviada a la petrolera";
                    cuerpo = String.format(
                        "<html><body>" +
                        "<h2>Estimado/a %s</h2>" +
                        "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido enviada a <strong>%s</strong> para su tramitacion.</p>" +
                        "<p>Matricula: <strong>%s</strong></p>" +
                        "<p>Cuando recibamos respuesta de la petrolera, le notificaremos el resultado.</p>" +
                        "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                        "</body></html>",
                        nombreSocio, tipoLabel, nombrePetrolera,
                        solicitud.getMatricula() != null ? solicitud.getMatricula() : "N/A");
                    break;
                default:
                    return;
            }
        }

        try {
            emailService.enviarCorreoHTML(emailSocio, asunto, cuerpo);
            registrarEnvioCorreo(solicitud, tipoNotificacion, emailSocio, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(solicitud, tipoNotificacion, emailSocio, false, e.getMessage());
            log.error("Error en notificacion {} al socio: {}", tipoNotificacion, e.getMessage());
        }
    }

    /**
     * Comprueba que la petrolera seleccionada puede recibir este tipo de solicitud.
     *
     * Regla general: un flag a null significa "sin restriccion configurada", por lo que se
     * permite (petroleras ya existentes no tienen valor y deben seguir funcionando igual).
     * Solo se bloquea cuando el administrador ha desmarcado explicitamente el flag.
     */
    private void validarRestriccionesPetrolera(CrearSolicitudDTO dto) {
        PetroleraDTO petrolera = obtenerPetroleraParaValidacion(dto.getPetroleraId());
        String nombre = petrolera.getNombre() != null ? petrolera.getNombre() : "seleccionada";

        if (!permitido(petrolera.getOperaDispositivos())) {
            throw new RuntimeException("La petrolera " + nombre + " no opera con dispositivos");
        }

        if (dto.getTipoSolicitud() == TipoSolicitud.SOLICITUD_CREDITO
                && !permitido(petrolera.getPermiteCreditoDispositivo())) {
            throw new RuntimeException(
                    "La petrolera " + nombre + " no admite solicitudes de credito para dispositivos");
        }
    }

    /** null = sin restriccion configurada => permitido. */
    private boolean permitido(Boolean flag) {
        return flag == null || flag;
    }

    /**
     * Obtiene la petrolera para validar. A diferencia de {@link #obtenerDatosPetrolera(Long)},
     * aqui no se usa un fallback: si no se puede leer la configuracion no se puede comprobar
     * la restriccion, y es preferible avisar al operador antes que crear una solicitud que el
     * procedimiento de ATG no permite.
     */
    private PetroleraDTO obtenerPetroleraParaValidacion(Long petroleraId) {
        PetroleraDTO petrolera;
        try {
            petrolera = restTemplate.getForObject(
                    petrolerasBaseUrl + "/api/petroleras/" + petroleraId, PetroleraDTO.class);
        } catch (Exception e) {
            log.error("Error al validar la petrolera {}: {}", petroleraId, e.getMessage());
            throw new RuntimeException(
                    "No se ha podido verificar la petrolera seleccionada. Intentelo de nuevo mas tarde.");
        }
        if (petrolera == null) {
            throw new RuntimeException("Petrolera no encontrada con id: " + petroleraId);
        }
        return petrolera;
    }

    private Map<String, Object> obtenerDatosSocio(Long socioId) {
        try {
            return restTemplate.getForObject(sociosBaseUrl + "/api/socios/" + socioId, Map.class);
        } catch (Exception e) {
            log.error("Error al obtener datos del socio: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("nombre", "Socio desconocido");
            fallback.put("email", "socio@example.com");
            return fallback;
        }
    }

    private Map<String, Object> obtenerDatosEmpresa(Long empresaId) {
        try {
            return restTemplate.getForObject(sociosBaseUrl + "/api/empresas/" + empresaId, Map.class);
        } catch (Exception e) {
            log.error("Error al obtener datos de la empresa: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> obtenerDatosPetrolera(Long petroleraId) {
        try {
            return restTemplate.getForObject(petrolerasBaseUrl + "/api/petroleras/" + petroleraId, Map.class);
        } catch (Exception e) {
            log.error("Error al obtener datos de la petrolera: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("nombre", "Petrolera desconocida");
            fallback.put("email", "admin@atg.com");
            return fallback;
        }
    }

    private Map<String, Object> obtenerPlantillaCorreo(Long petroleraId, String tipoPlantilla) {
        try {
            String url = String.format("%s/api/plantillas-correo/petrolera/%d?tipo=%s",
                petrolerasBaseUrl, petroleraId, tipoPlantilla);
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("No se encontro plantilla de correo para petrolera {} tipo {}", petroleraId, tipoPlantilla);
            return null;
        }
    }

    private SolicitudDispositivoDTO convertirADTO(SolicitudDispositivo solicitud) {
        SolicitudDispositivoDTO dto = new SolicitudDispositivoDTO();
        dto.setId(solicitud.getId());
        dto.setNumeroSolicitud(solicitud.getNumeroSolicitud());
        dto.setSocioId(solicitud.getSocioId());
        dto.setEmpresaId(solicitud.getEmpresaId());
        dto.setPetroleraId(solicitud.getPetroleraId());
        dto.setDispositivoId(solicitud.getDispositivoId());
        dto.setTipoSolicitud(solicitud.getTipoSolicitud());
        dto.setEstado(solicitud.getEstado());
        dto.setMatricula(solicitud.getMatricula());
        dto.setMatriculaDestino(solicitud.getMatriculaDestino());
        dto.setMonto(solicitud.getMonto());
        dto.setMontoConcedido(solicitud.getMontoConcedido());
        dto.setObservaciones(solicitud.getObservaciones());
        dto.setRutaPdfEditable(solicitud.getRutaPdfEditable());
        dto.setNombrePdfEditable(solicitud.getNombrePdfEditable());
        dto.setRutaPdfEnviado(solicitud.getRutaPdfEnviado());
        dto.setNombrePdfEnviado(solicitud.getNombrePdfEnviado());
        dto.setRutaPdfFirmado(solicitud.getRutaPdfFirmado());
        dto.setNombrePdfFirmado(solicitud.getNombrePdfFirmado());
        dto.setRutaPdfFinal(solicitud.getRutaPdfFinal());
        dto.setNombrePdfFinal(solicitud.getNombrePdfFinal());
        dto.setFechaEnvioSocio(solicitud.getFechaEnvioSocio());
        dto.setFechaRecepcionFirmado(solicitud.getFechaRecepcionFirmado());
        dto.setMotivoRechazo(solicitud.getMotivoRechazo());
        dto.setFechaEnvioPetrolera(solicitud.getFechaEnvioPetrolera());
        dto.setFechaRespuestaPetrolera(solicitud.getFechaRespuestaPetrolera());
        dto.setFechaNotificacionSocio(solicitud.getFechaNotificacionSocio());
        dto.setRespuestaPetrolera(solicitud.getRespuestaPetrolera());
        dto.setProgramadoEnvio(solicitud.getProgramadoEnvio());
        dto.setFechaProgramadaEnvio(solicitud.getFechaProgramadaEnvio());
        dto.setCorreosEnviados(solicitud.getCorreosEnviados());
        dto.setCreatedAt(solicitud.getCreatedAt());
        dto.setUpdatedAt(solicitud.getUpdatedAt());

        // Enrichment
        try {
            Map<String, Object> socio = obtenerDatosSocio(solicitud.getSocioId());
            dto.setSocioNombre((String) socio.get("nombre"));
            dto.setSocioEmail((String) socio.get("email"));

            if (solicitud.getEmpresaId() != null) {
                Map<String, Object> empresa = obtenerDatosEmpresa(solicitud.getEmpresaId());
                dto.setEmpresaNombre((String) empresa.get("nombre"));
                dto.setEmpresaEmail((String) empresa.get("email"));
            }

            Map<String, Object> petrolera = obtenerDatosPetrolera(solicitud.getPetroleraId());
            dto.setPetroleraNombre((String) petrolera.get("nombre"));
            dto.setPetroleraEmail((String) petrolera.get("email"));

            if (solicitud.getDispositivoId() != null) {
                try {
                    Dispositivo dispositivo = dispositivoRepository.findById(solicitud.getDispositivoId()).orElse(null);
                    if (dispositivo != null) {
                        dto.setDispositivoMatricula(dispositivo.getMatricula());
                    }
                } catch (Exception e) {
                    log.warn("Error al obtener dispositivo: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error al obtener datos relacionados: {}", e.getMessage());
        }

        return dto;
    }
}
