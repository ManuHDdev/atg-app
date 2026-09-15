package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.client.PetrolerasClient;
import com.manuhd.app.tarjetas.dto.*;
import com.manuhd.app.tarjetas.exception.BusinessValidationException;
import com.manuhd.app.tarjetas.model.*;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import com.manuhd.app.tarjetas.security.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudTarjetaService {

    private final SolicitudTarjetaRepository repository;
    private final PlantillaTarjetaService plantillaService;
    private final EmailService emailService;
    private final TarjetaService tarjetaService;
    private final RestTemplate restTemplate;
    private final UsuarioActualService usuarioActual;
    private final PdfService pdfService;
    private final PetrolerasClient petrolerasClient;

    @Value("${app.socios.url:http://localhost:8081}")
    private String sociosServiceUrl;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasServiceUrl;

    @Transactional(readOnly = true)
    public List<SolicitudTarjetaDTO> findAll() {
        log.info("Obteniendo todas las solicitudes de tarjetas");
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SolicitudTarjetaDTO findById(Long id) {
        log.info("Buscando solicitud con id: {}", id);
        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));
        return convertToDTO(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudTarjetaDTO> findBySocioId(Long socioId) {
        log.info("Buscando solicitudes del socio: {}", socioId);
        return repository.findBySocioId(socioId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudTarjetaDTO> findByEstado(EstadoSolicitud estado) {
        log.info("Buscando solicitudes con estado: {}", estado);
        return repository.findByEstado(estado).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudTarjetaDTO create(CrearSolicitudDTO dto) {
        log.info("Creando nueva solicitud de tipo: {} para socio: {}", dto.getTipo(), dto.getSocioId());

        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setSocioId(dto.getSocioId());
        solicitud.setPetroleraId(dto.getPetroleraId());
        solicitud.setMatricula(dto.getMatricula());
        solicitud.setNumeroContrato(dto.getNumeroContrato());
        solicitud.setTipo(dto.getTipo());
        solicitud.setObservaciones(dto.getObservaciones());
        solicitud.setSolicitadoPor(dto.getSolicitadoPor());
        solicitud.setTarjetaId(dto.getTarjetaId());
        solicitud.setFechaSolicitud(LocalDateTime.now());

        // El impreso de duplicado es el del alta con el motivo escrito a mano: sin motivo
        // la solicitud no se puede presentar a la petrolera. En el resto de tipos el campo
        // no aplica y se deja a null aunque el cliente lo mande.
        if (dto.getTipo() == TipoSolicitud.DUPLICADO) {
            if (dto.getMotivoDuplicado() == null) {
                throw new RuntimeException("El motivo del duplicado es obligatorio (deterioro o extravío)");
            }
            solicitud.setMotivoDuplicado(dto.getMotivoDuplicado());
        }

        // Una LLEGADA no es una petición que haya que tramitar: es el registro de que las
        // tarjetas ya han llegado. Nace directamente en TARJETA_LLEGADA, con su fecha, y el
        // correo de aviso al socio (recogida en Madrid / envío postal fuera) sale una sola
        // vez, en este mismo momento. Lo único que queda después es registrar la entrega.
        if (dto.getTipo() == TipoSolicitud.LLEGADA) {
            if (dto.getFechaLlegadaEstimada() == null) {
                throw new RuntimeException("La fecha de llegada es obligatoria para registrar la llegada de tarjetas");
            }
            solicitud.setFechaLlegadaEstimada(dto.getFechaLlegadaEstimada());
            solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
            solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
            solicitud.setNumeroSolicitud(generarNumeroSolicitud());
        } else {
            // El resto de tipos sí llevan papeleo: nacen como borrador con el impreso de la
            // petrolera ya descargado, y solo llegan a PENDIENTE cuando el socio lo devuelve
            // firmado y se presenta a la petrolera.
            String numeroSolicitud = generarNumeroSolicitud();
            solicitud.setNumeroSolicitud(numeroSolicitud);
            solicitud.setEstado(EstadoSolicitud.BORRADOR);
            solicitud.setRutaPdfEditable(descargarPlantillaDeSolicitud(dto.getPetroleraId(), dto.getTipo(), numeroSolicitud));
            solicitud.setNombrePdfEditable(PdfService.EDITABLE);
        }

        SolicitudTarjeta saved = repository.save(solicitud);
        log.info("Solicitud creada con id: {}", saved.getId());

        // Enviar correos automáticos según el tipo y registrar envíos
        StringBuilder correosEnviados = new StringBuilder();
        try {
            List<EnvioCorreoResult> resultados = enviarCorreosAutomaticos(saved);
            for (EnvioCorreoResult resultado : resultados) {
                correosEnviados.append(resultado.toString()).append("\n");
            }
            saved.setCorreosEnviados(correosEnviados.toString());
            repository.save(saved);
        } catch (Exception e) {
            log.error("Error al enviar correos para solicitud {}: {}", saved.getId(), e.getMessage());
            correosEnviados.append("ERROR: ").append(e.getMessage());
            saved.setCorreosEnviados(correosEnviados.toString());
            repository.save(saved);
        }

        return convertToDTO(saved);
    }

    // ---------- circuito del documento firmado ----------

    /**
     * Sustituye el impreso editable por el que ha rellenado la oficina. Solo tiene sentido
     * mientras la solicitud sigue en BORRADOR: después el documento ya está en manos del socio.
     */
    @Transactional
    public SolicitudTarjetaDTO guardarPdfEditado(Long id, MultipartFile pdfEditado) throws IOException {
        log.info("Guardando PDF editado de la solicitud: {}", id);

        SolicitudTarjeta solicitud = obtenerSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new BusinessValidationException("Solo se puede editar el impreso de una solicitud en borrador");
        }

        String ruta = pdfService.guardarPdfEditado(pdfEditado, exigirNumeroSolicitud(solicitud));
        solicitud.setRutaPdfEditable(ruta);
        solicitud.setNombrePdfEditable(pdfEditado.getOriginalFilename());

        return convertToDTO(repository.save(solicitud));
    }

    /**
     * Manda al socio el impreso aplanado para que lo firme. El socio firma fuera del sistema
     * y devuelve el documento por el canal que prefiera; aquí solo se deja constancia del envío.
     */
    @Transactional
    public SolicitudTarjetaDTO enviarASocio(Long id) throws IOException {
        log.info("Enviando a socio el impreso de la solicitud: {}", id);

        SolicitudTarjeta solicitud = obtenerSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new BusinessValidationException("Solo se puede enviar al socio una solicitud en borrador");
        }

        String numeroSolicitud = exigirNumeroSolicitud(solicitud);
        String mensajeFaltaPdf = "No se puede enviar al socio: falta el impreso de la solicitud " + numeroSolicitud;

        // Sin impreso no hay nada que firmar: se comprueba ANTES de dar el envío por hecho.
        validarPdfDisponible(solicitud.getRutaPdfEditable(), mensajeFaltaPdf);

        String rutaPdfEnviado = pdfService.aplanarPdfParaSolicitud(solicitud.getRutaPdfEditable(), numeroSolicitud);
        Path adjunto = validarPdfDisponible(rutaPdfEnviado, mensajeFaltaPdf);

        solicitud.setRutaPdfEnviado(rutaPdfEnviado);
        solicitud.setNombrePdfEnviado(PdfService.ENVIADO);
        solicitud.setEstado(EstadoSolicitud.ENVIADO_SOCIO);
        solicitud.setFechaEnvioSocio(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());

        SolicitudTarjeta updated = repository.save(solicitud);

        // Un fallo de correo no deshace el envío: el documento ya está aplanado y la etapa
        // avanzada. El resultado queda registrado en el historial de correos.
        try {
            SocioDTO socio = obtenerSocio(updated.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(updated.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, updated);

            enviarCorreoConAdjuntoSiHayPlantilla(updated, TipoPlantilla.DOCUMENTO_SOCIO, socio.getEmail(),
                    variables, adjunto, nombreAdjunto("Solicitud", updated));
        } catch (Exception e) {
            log.error("Error al enviar el impreso al socio: {}", e.getMessage());
            registrarCorreo(updated, new EnvioCorreoResult(false, TipoPlantilla.DOCUMENTO_SOCIO.name(),
                    "socio", e.getMessage()));
        }

        return convertToDTO(updated);
    }

    /**
     * Guarda el escaneado que ha devuelto el socio. No cambia el estado a propósito: el
     * escaneado puede venir torcido o incompleto y se sustituye tantas veces como haga falta;
     * el paso lo cierra explícitamente {@link #aceptarFirmaSocio(Long)}.
     */
    @Transactional
    public SolicitudTarjetaDTO subirPdfFirmado(Long id, MultipartFile pdfFirmado) throws IOException {
        log.info("Subiendo el impreso firmado de la solicitud: {}", id);

        SolicitudTarjeta solicitud = obtenerSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new BusinessValidationException(
                    "Solo se puede subir el impreso firmado de una solicitud enviada al socio");
        }

        solicitud.setRutaPdfFirmado(pdfService.guardarPdfFirmado(pdfFirmado, exigirNumeroSolicitud(solicitud)));
        solicitud.setNombrePdfFirmado(pdfFirmado.getOriginalFilename());
        solicitud.setFechaRecepcionFirmado(LocalDateTime.now());

        return convertToDTO(repository.save(solicitud));
    }

    /** Da por buena la firma recibida y deja la solicitud lista para presentarla a la petrolera. */
    @Transactional
    public SolicitudTarjetaDTO aceptarFirmaSocio(Long id) {
        log.info("Aceptando la firma del socio en la solicitud: {}", id);

        SolicitudTarjeta solicitud = obtenerSolicitud(id);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new BusinessValidationException(
                    "Solo se puede aceptar la firma de una solicitud enviada al socio");
        }

        validarPdfDisponible(solicitud.getRutaPdfFirmado(),
                "Debe subir el impreso firmado antes de aceptar la firma");

        solicitud.setEstado(EstadoSolicitud.FIRMADO_SOCIO);
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());

        return convertToDTO(repository.save(solicitud));
    }

    /**
     * Presenta la solicitud a la petrolera: un correo por solicitud, con el impreso firmado
     * adjunto y los datos del socio en el cuerpo. Nunca se agrupan varias en un mismo envío.
     */
    @Transactional
    public SolicitudTarjetaDTO enviarAPetrolera(Long id) throws IOException {
        log.info("Enviando a petrolera la solicitud: {}", id);

        SolicitudTarjeta solicitud = obtenerSolicitud(id);

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
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());

        SolicitudTarjeta updated = repository.save(solicitud);

        try {
            SocioDTO socio = obtenerSocio(updated.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(updated.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, updated);

            String asunto;
            String cuerpo;
            Optional<PlantillaTarjeta> plantilla = plantillaService.buscarPlantillaActiva(TipoPlantilla.DOCUMENTO_PETROLERA);
            if (plantilla.isPresent()) {
                asunto = plantilla.get().getAsunto();
                cuerpo = plantilla.get().getCuerpo();
            } else {
                // Este correo no puede dejar de salir por una plantilla sin configurar: es el
                // que presenta la solicitud a la petrolera.
                log.warn("No hay plantilla activa para {}; se usa el texto por defecto",
                        TipoPlantilla.DOCUMENTO_PETROLERA);
                asunto = asuntoPetroleraPorDefecto(updated);
                cuerpo = cuerpoPetroleraPorDefecto(updated);
            }

            EnvioCorreoResult resultado = emailService.enviarCorreoConPlantillaYAdjunto(
                    petrolera.getEmail(), asunto, cuerpo, variables,
                    TipoPlantilla.DOCUMENTO_PETROLERA.name(), adjunto,
                    nombreAdjunto("Solicitud-firmada", updated));
            registrarCorreo(updated, resultado);
        } catch (Exception e) {
            log.error("Error al enviar la solicitud a la petrolera: {}", e.getMessage());
            registrarCorreo(updated, new EnvioCorreoResult(false, TipoPlantilla.DOCUMENTO_PETROLERA.name(),
                    "petrolera", e.getMessage()));
        }

        return convertToDTO(updated);
    }

    /** Devuelve el PDF de una etapa concreta del circuito. */
    @Transactional(readOnly = true)
    public byte[] descargarPdf(Long id, TipoPdf tipo) throws IOException {
        SolicitudTarjeta solicitud = obtenerSolicitud(id);

        String rutaPdf = switch (tipo) {
            case EDITABLE -> solicitud.getRutaPdfEditable();
            case ENVIADO -> solicitud.getRutaPdfEnviado();
            case FIRMADO -> solicitud.getRutaPdfFirmado();
            case FINAL -> solicitud.getRutaPdfFinal();
        };

        if (rutaPdf == null) {
            throw new BusinessValidationException("El PDF " + tipo + " no está disponible para esta solicitud");
        }

        return pdfService.leerPdf(rutaPdf);
    }

    /** Etapas del circuito que tienen un PDF descargable. */
    public enum TipoPdf {
        EDITABLE, ENVIADO, FIRMADO, FINAL
    }

    /**
     * Registra que la petrolera ha denegado la solicitud. ATG no decide: solo deja constancia
     * de la respuesta recibida y avisa al socio.
     */
    @Transactional
    public SolicitudTarjetaDTO denegarPorPetrolera(Long id, String motivo) {
        log.info("Registrando denegación de la petrolera para la solicitud con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se puede registrar la respuesta de la petrolera en solicitudes pendientes");
        }

        // RECHAZADA sigue siendo el valor persistido (denegada por la petrolera): renombrarlo
        // exigiría migrar datos sin ganancia real, así que solo cambia la etiqueta de la UI.
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        solicitud.setObservaciones(motivo);
        // El motivo se guarda además en su propio campo: observaciones es un cajón compartido
        // que cualquier paso posterior puede sobrescribir.
        solicitud.setMotivoRechazo(motivo);

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Denegación de la petrolera registrada en la solicitud con id: {}", updated.getId());

        // Avisar al socio del rechazo (si existe la plantilla)
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);
            variables.put("motivo", motivo != null ? motivo : "");

            Optional<EnvioCorreoResult> resultado = enviarCorreoSiHayPlantilla(
                    TipoPlantilla.ALTA_RECHAZADA, socio.getEmail(), variables);
            if (resultado.isPresent()) {
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.get().toString());
                updated.setCorreosEnviados(correosEnviados.toString());
                repository.save(updated);
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de denegación: {}", e.getMessage());
            if (correosEnviados.length() > 0) correosEnviados.append("\n");
            correosEnviados.append("ERROR: ").append(e.getMessage());
            updated.setCorreosEnviados(correosEnviados.toString());
            repository.save(updated);
        }

        return convertToDTO(updated);
    }

    /**
     * Registra que la petrolera ha aprobado la solicitud. ATG no aprueba nada: presenta la
     * solicitud a la petrolera y aquí deja constancia de la respuesta que ha recibido.
     */
    @Transactional
    public SolicitudTarjetaDTO aprobarPorPetrolera(Long id) {
        log.info("Registrando aprobación de la petrolera para la solicitud con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se puede registrar la respuesta de la petrolera en solicitudes pendientes");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Aprobación de la petrolera registrada en la solicitud con id: {}", updated.getId());

        // Enviar correo de aprobación al socio (si existe la plantilla)
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);

            Optional<EnvioCorreoResult> resultado = enviarCorreoSiHayPlantilla(
                    TipoPlantilla.ALTA_APROBADA, socio.getEmail(), variables);
            if (resultado.isPresent()) {
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.get().toString());
                updated.setCorreosEnviados(correosEnviados.toString());
                repository.save(updated);
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de aprobación: {}", e.getMessage());
            if (correosEnviados.length() > 0) correosEnviados.append("\n");
            correosEnviados.append("ERROR: ").append(e.getMessage());
            updated.setCorreosEnviados(correosEnviados.toString());
            repository.save(updated);
        }

        return convertToDTO(updated);
    }

    /** Registra la baja confirmada por la petrolera y cierra la solicitud. */
    @Transactional
    public SolicitudTarjetaDTO aprobarBajaPorPetrolera(Long id, AprobarBajaDTO dto) {
        log.info("Registrando aprobación de la petrolera para la solicitud de BAJA con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se puede registrar la respuesta de la petrolera en solicitudes pendientes");
        }

        if (solicitud.getTipo() != TipoSolicitud.BAJA) {
            throw new RuntimeException("Este método solo es para solicitudes de BAJA");
        }

        if (solicitud.getTarjetaId() == null) {
            throw new RuntimeException("La solicitud de BAJA debe tener una tarjeta asociada");
        }

        // Buscar y dar de baja la tarjeta
        Tarjeta tarjeta = tarjetaService.findById(solicitud.getTarjetaId());
        tarjeta.setActiva(false);
        tarjeta.setFechaBaja(dto.getFechaBaja());
        tarjetaService.update(tarjeta.getId(), tarjeta);

        log.info("Tarjeta {} dada de baja con fecha: {}", tarjeta.getId(), dto.getFechaBaja());

        // Marcar solicitud como COMPLETADA (no APROBADA, porque ya terminó el proceso)
        solicitud.setEstado(EstadoSolicitud.COMPLETADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        if (dto.getObservaciones() != null && !dto.getObservaciones().isEmpty()) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud de BAJA completada con id: {}", updated.getId());

        // Enviar correo de confirmación de baja al socio
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);
            variables.put("fechaBaja", dto.getFechaBaja().toString());

            // BAJA_CONFIRMADA (no BAJA_SOCIO): la petrolera ya ha confirmado la baja,
            // el correo de "hemos tramitado tu solicitud" se envió al crear la solicitud.
            Optional<EnvioCorreoResult> resultado = enviarCorreoSiHayPlantilla(
                    TipoPlantilla.BAJA_CONFIRMADA, socio.getEmail(), variables);
            if (resultado.isPresent()) {
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.get().toString());
                updated.setCorreosEnviados(correosEnviados.toString());
                repository.save(updated);
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de baja: {}", e.getMessage());
            if (correosEnviados.length() > 0) correosEnviados.append("\n");
            correosEnviados.append("ERROR: ").append(e.getMessage());
            updated.setCorreosEnviados(correosEnviados.toString());
            repository.save(updated);
        }

        return convertToDTO(updated);
    }

    /**
     * Registra el duplicado confirmado por la petrolera. No cierra la solicitud: un duplicado
     * es una tarjeta física que todavía tiene que llegar y entregarse al socio, igual que un
     * alta, así que queda APROBADA a la espera de registrar su llegada.
     */
    @Transactional
    public SolicitudTarjetaDTO aprobarDuplicadoPorPetrolera(Long id, AprobarDuplicadoDTO dto) {
        log.info("Registrando aprobación de la petrolera para la solicitud de DUPLICADO con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se puede registrar la respuesta de la petrolera en solicitudes pendientes");
        }

        if (solicitud.getTipo() != TipoSolicitud.DUPLICADO) {
            throw new RuntimeException("Este método solo es para solicitudes de DUPLICADO");
        }

        if (solicitud.getTarjetaId() == null) {
            throw new RuntimeException("La solicitud de DUPLICADO debe tener una tarjeta asociada");
        }

        // Se consulta la tarjeta para comprobar que existe y para poder anunciar al socio con
        // cuántas tarjetas se quedará. El incremento real NO se aplica aquí: la tarjeta física
        // todavía no existe. Se aplica al entregarla (marcarEntregada).
        Tarjeta tarjeta = tarjetaService.findById(solicitud.getTarjetaId());
        int cantidadTrasElDuplicado = (tarjeta.getCantidad() != null ? tarjeta.getCantidad() : 1) + 1;

        log.info("Duplicado confirmado por la petrolera para la tarjeta {}. Cantidad prevista tras la entrega: {}",
                tarjeta.getId(), cantidadTrasElDuplicado);

        // Queda APROBADA: aún falta registrar la llegada y la entrega al socio
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        if (dto.getObservaciones() != null && !dto.getObservaciones().isEmpty()) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud de DUPLICADO aprobada por la petrolera con id: {}", updated.getId());

        // Enviar correo de confirmación al socio
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);
            variables.put("fechaRespuesta", dto.getFechaRespuesta().toString());
            variables.put("cantidad", String.valueOf(cantidadTrasElDuplicado));

            // DUPLICADO_CONFIRMADA (no DUPLICADO_SOCIO): la petrolera ya ha confirmado el
            // duplicado, el correo de trámite se envió al crear la solicitud.
            Optional<EnvioCorreoResult> resultado = enviarCorreoSiHayPlantilla(
                    TipoPlantilla.DUPLICADO_CONFIRMADA, socio.getEmail(), variables);
            if (resultado.isPresent()) {
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.get().toString());
                updated.setCorreosEnviados(correosEnviados.toString());
                repository.save(updated);
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de duplicado: {}", e.getMessage());
            if (correosEnviados.length() > 0) correosEnviados.append("\n");
            correosEnviados.append("ERROR: ").append(e.getMessage());
            updated.setCorreosEnviados(correosEnviados.toString());
            repository.save(updated);
        }

        return convertToDTO(updated);
    }

    /**
     * Paso intermedio del ALTA y del DUPLICADO: la petrolera ya los aprobó y ahora llega la
     * tarjeta física, así que hay que avisar al socio (recogida en Madrid / envío postal fuera).
     * No aplica a una BAJA, donde no llega nada, ni a una solicitud de tipo LLEGADA, que nace ya
     * en TARJETA_LLEGADA con su correo enviado; volver a pasar por aquí duplicaría el aviso.
     */
    @Transactional
    public SolicitudTarjetaDTO registrarLlegada(Long id, RegistrarLlegadaDTO dto) {
        log.info("Registrando llegada de tarjeta para solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getTipo() == TipoSolicitud.LLEGADA) {
            throw new RuntimeException("Una solicitud de LLEGADA ya nace con la llegada registrada: no se puede registrar otra vez");
        }

        if (solicitud.getTipo() == TipoSolicitud.BAJA) {
            throw new RuntimeException("Una solicitud de BAJA no espera ninguna tarjeta: no se puede registrar su llegada");
        }

        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new RuntimeException("Solo se puede registrar llegada de solicitudes aprobadas");
        }

        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        solicitud.setFechaLlegadaEstimada(dto.getFechaLlegadaEstimada());
        solicitud.setNumeroContrato(dto.getNumeroContrato());  // Actualizar numeroContrato si se proporciona
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        if (dto.getObservaciones() != null) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Llegada registrada para solicitud: {}", updated.getId());

        // Enviar correo de llegada según provincia
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);

            List<EnvioCorreoResult> resultados = enviarCorreoLlegada(socio, variables);
            for (EnvioCorreoResult resultado : resultados) {
                correosEnviados.append(resultado.toString()).append("\n");
            }
            updated.setCorreosEnviados(correosEnviados.toString());
            repository.save(updated);
        } catch (Exception e) {
            log.error("Error al enviar correo de llegada: {}", e.getMessage());
        }

        return convertToDTO(updated);
    }

    /**
     * Entregar la tarjeta al socio es el último paso: registra la fecha de entrega y cierra la
     * solicitud. No hay un "finalizar" posterior, porque no aportaba nada más que otro clic.
     */
    @Transactional
    public SolicitudTarjetaDTO marcarEntregada(Long id, MarcarEntregadaDTO dto) {
        log.info("Marcando como entregada la solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.TARJETA_LLEGADA) {
            throw new RuntimeException("Solo se puede marcar como entregada si la tarjeta ha llegado");
        }

        solicitud.setEstado(EstadoSolicitud.COMPLETADA);
        solicitud.setFechaEntrega(dto.getFechaEntrega() != null ? dto.getFechaEntrega() : LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        if (dto.getObservaciones() != null) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        // El efecto físico se aplica al entregar, que es cuando el socio tiene la tarjeta en la
        // mano: el ALTA crea una Tarjeta nueva y el DUPLICADO suma una unidad a la existente
        // (no genera fila propia, porque es la misma tarjeta repetida).
        if (solicitud.getTipo() == TipoSolicitud.ALTA) {
            crearTarjeta(solicitud);
        } else if (solicitud.getTipo() == TipoSolicitud.DUPLICADO) {
            incrementarCantidadTarjeta(solicitud);
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud entregada y completada: {}", updated.getId());

        return convertToDTO(updated);
    }

    private List<EnvioCorreoResult> enviarCorreosAutomaticos(SolicitudTarjeta solicitud) {
        log.info("Enviando correos automáticos para solicitud: {}", solicitud.getId());

        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();

        SocioDTO socio = obtenerSocio(solicitud.getSocioId());
        PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());

        Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);

        switch (solicitud.getTipo()) {
            case LLEGADA:
                resultados.addAll(enviarCorreoLlegada(socio, variables));
                break;
            case ALTA:
                resultados.addAll(enviarCorreoAlta(socio, variables));
                break;
            case BAJA:
                resultados.addAll(enviarCorreoBaja(socio, variables));
                break;
            case DUPLICADO:
                resultados.addAll(enviarCorreoDuplicado(socio, variables));
                break;
        }

        return resultados;
    }

    // ---------- apoyo del circuito del documento firmado ----------

    private SolicitudTarjeta obtenerSolicitud(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));
    }

    /**
     * El número de solicitud da nombre al directorio de sus PDFs. Las solicitudes creadas
     * antes del circuito de firma no lo tienen, así que no pueden entrar en él.
     */
    private String exigirNumeroSolicitud(SolicitudTarjeta solicitud) {
        String numeroSolicitud = solicitud.getNumeroSolicitud();
        if (numeroSolicitud == null || numeroSolicitud.isBlank()) {
            throw new BusinessValidationException("La solicitud no tiene número asignado: se creó antes del "
                    + "circuito de firma y su documentación debe tramitarse fuera del sistema");
        }
        return numeroSolicitud;
    }

    private String generarNumeroSolicitud() {
        String prefijo = "TAR-" + Year.now().getValue() + "-";
        Integer maxNumero = repository.findMaxNumeroSolicitudByYear(prefijo);
        return String.format("%s%05d", prefijo, (maxNumero != null ? maxNumero : 0) + 1);
    }

    /**
     * Descarga de la petrolera el impreso del tipo de solicitud y lo deja en el directorio
     * de la solicitud. Sin impreso no hay nada que firmar, así que la creación se detiene.
     *
     * @return la ruta del PDF editable recién creado
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
                    "No se pudo obtener el impreso de la petrolera. Inténtelo de nuevo en unos momentos.");
        }
    }

    /**
     * Comprueba que el PDF que se va a adjuntar existe y es legible antes de dar el envío
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

    /** Añade una línea al historial de correos de la solicitud y lo persiste. */
    private void registrarCorreo(SolicitudTarjeta solicitud, EnvioCorreoResult resultado) {
        StringBuilder historial = new StringBuilder(
                solicitud.getCorreosEnviados() != null ? solicitud.getCorreosEnviados() : "");
        if (historial.length() > 0) {
            historial.append("\n");
        }
        historial.append(resultado.toString());
        solicitud.setCorreosEnviados(historial.toString());
        repository.save(solicitud);
    }

    /**
     * Variante con adjunto de {@link #enviarCorreoSiHayPlantilla}: si no hay plantilla activa
     * deja constancia en el log y no interrumpe la transición.
     */
    private void enviarCorreoConAdjuntoSiHayPlantilla(SolicitudTarjeta solicitud, TipoPlantilla tipo,
                                                      String destinatario, Map<String, String> variables,
                                                      Path adjunto, String nombreAdjunto) {
        Optional<PlantillaTarjeta> plantilla = plantillaService.buscarPlantillaActiva(tipo);
        if (plantilla.isEmpty()) {
            log.warn("No hay plantilla activa para {}; no se envía correo", tipo);
            return;
        }

        registrarCorreo(solicitud, emailService.enviarCorreoConPlantillaYAdjunto(
                destinatario, plantilla.get().getAsunto(), plantilla.get().getCuerpo(),
                variables, tipo.name(), adjunto, nombreAdjunto));
    }

    private String nombreAdjunto(String prefijo, SolicitudTarjeta solicitud) {
        return prefijo + "-" + solicitud.getNumeroSolicitud() + ".pdf";
    }

    private String asuntoPetroleraPorDefecto(SolicitudTarjeta solicitud) {
        return "Solicitud de tarjeta " + solicitud.getTipo() + " - " + solicitud.getNumeroSolicitud()
                + " - Matrícula " + solicitud.getMatricula();
    }

    /**
     * Cuerpo de respaldo del correo a la petrolera, con los datos del socio que la petrolera
     * necesita para tramitar la solicitud. Las variables las resuelve después el EmailService.
     */
    private String cuerpoPetroleraPorDefecto(SolicitudTarjeta solicitud) {
        return "<html><body>"
                + "<h2>Solicitud de tarjeta - " + solicitud.getTipo() + "</h2>"
                + "<p>Estimados,</p>"
                + "<p>Adjuntamos la solicitud firmada por el socio con los siguientes datos:</p>"
                + "<table style='border-collapse: collapse; margin: 20px 0;'>"
                + filaPetrolera("Nº Solicitud", solicitud.getNumeroSolicitud())
                + filaPetrolera("Socio", "{nombre}")
                + filaPetrolera("Nº de socio", "{nif}")
                + filaPetrolera("Dirección", "{direccionCompleta}")
                + filaPetrolera("Teléfono", "{telefono}")
                + filaPetrolera("Correo", "{email}")
                + filaPetrolera("Matrícula", "{matricula}")
                + filaPetrolera("Nº de contrato", "{numeroContrato}")
                + "</table>"
                + "<p>Saludos cordiales,<br/>Sistema de Gestión ATG</p>"
                + "</body></html>";
    }

    private String filaPetrolera(String etiqueta, String valor) {
        return "<tr><td style='padding: 8px; font-weight: bold;'>" + etiqueta + ":</td>"
                + "<td style='padding: 8px;'>" + (valor != null ? valor : "") + "</td></tr>";
    }

    /**
     * Envía un correo solo si hay plantilla activa para el tipo indicado. Si todavía no
     * está configurada, deja constancia en el log y no interrumpe la transición de estado
     * ni ensucia el historial de correos enviados.
     */
    private Optional<EnvioCorreoResult> enviarCorreoSiHayPlantilla(TipoPlantilla tipo, String destinatario, Map<String, String> variables) {
        Optional<PlantillaTarjeta> plantilla = plantillaService.buscarPlantillaActiva(tipo);
        if (plantilla.isEmpty()) {
            log.warn("No hay plantilla activa para {}; no se envía correo", tipo);
            return Optional.empty();
        }

        return Optional.of(emailService.enviarCorreoConPlantilla(
                destinatario,
                plantilla.get().getAsunto(),
                plantilla.get().getCuerpo(),
                variables,
                tipo.name()
        ));
    }

    /**
     * Detecta si el socio pertenece a la Comunidad de Madrid tolerando mayúsculas,
     * espacios, acentos y variantes como "Comunidad de Madrid".
     */
    boolean esProvinciaMadrid(String provincia) {
        if (provincia == null || provincia.isBlank()) {
            return false;
        }

        String normalizada = Normalizer.normalize(provincia.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();

        // contains cubre tanto "MADRID" exacto como "COMUNIDAD DE MADRID"
        return normalizada.contains("MADRID");
    }

    private List<EnvioCorreoResult> enviarCorreoLlegada(SocioDTO socio, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        TipoPlantilla tipoPlantilla;

        if (esProvinciaMadrid(socio.getProvincia())) {
            tipoPlantilla = TipoPlantilla.LLEGADA_MADRID;
            log.info("Enviando correo de llegada para Madrid al socio: {}", socio.getNombre());
        } else {
            tipoPlantilla = TipoPlantilla.LLEGADA_FUERA;
            log.info("Enviando correo de llegada fuera de Madrid al socio: {}", socio.getNombre());
        }

        PlantillaTarjeta plantilla = plantillaService.obtenerPlantillaActiva(tipoPlantilla);
        EnvioCorreoResult resultado = emailService.enviarCorreoConPlantilla(
                socio.getEmail(),
                plantilla.getAsunto(),
                plantilla.getCuerpo(),
                variables,
                tipoPlantilla.name()
        );
        resultados.add(resultado);
        return resultados;
    }

    /**
     * IMPORTANTE - NO REINTRODUCIR EL CORREO A LA PETROLERA AQUI:
     * al crearse, la solicitud nace en BORRADOR y todavia no se ha presentado nada.
     * La petrolera se entera en enviarAPetrolera(), que es cuando sale el documento
     * firmado por el socio (DOCUMENTO_PETROLERA). Avisarla tambien al crear duplicaba
     * el envio y anunciaba una solicitud que aun no existia para ella.
     */
    private List<EnvioCorreoResult> enviarCorreoAlta(SocioDTO socio, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        log.info("Enviando correo de alta al socio: {}", socio.getNombre());

        PlantillaTarjeta plantillaSocio = plantillaService.obtenerPlantillaActiva(TipoPlantilla.ALTA_SOCIO);
        EnvioCorreoResult resultadoSocio = emailService.enviarCorreoConPlantilla(
                socio.getEmail(),
                plantillaSocio.getAsunto(),
                plantillaSocio.getCuerpo(),
                variables,
                TipoPlantilla.ALTA_SOCIO.name()
        );
        resultados.add(resultadoSocio);
        return resultados;
    }

    private List<EnvioCorreoResult> enviarCorreoBaja(SocioDTO socio, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        log.info("Enviando correo de baja al socio: {}", socio.getNombre());

        PlantillaTarjeta plantilla = plantillaService.obtenerPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
        EnvioCorreoResult resultado = emailService.enviarCorreoConPlantilla(
                socio.getEmail(),
                plantilla.getAsunto(),
                plantilla.getCuerpo(),
                variables,
                TipoPlantilla.BAJA_SOCIO.name()
        );
        resultados.add(resultado);
        return resultados;
    }

    private List<EnvioCorreoResult> enviarCorreoDuplicado(SocioDTO socio, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        log.info("Enviando correo de duplicado al socio: {}", socio.getNombre());

        PlantillaTarjeta plantilla = plantillaService.obtenerPlantillaActiva(TipoPlantilla.DUPLICADO_SOCIO);
        EnvioCorreoResult resultado = emailService.enviarCorreoConPlantilla(
                socio.getEmail(),
                plantilla.getAsunto(),
                plantilla.getCuerpo(),
                variables,
                TipoPlantilla.DUPLICADO_SOCIO.name()
        );
        resultados.add(resultado);
        return resultados;
    }

    private void crearTarjeta(SolicitudTarjeta solicitud) {
        log.info("Creando tarjeta para solicitud: {}", solicitud.getId());

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setSocioId(solicitud.getSocioId());
        tarjeta.setPetroleraId(solicitud.getPetroleraId());
        tarjeta.setMatricula(solicitud.getMatricula());
        tarjeta.setSolicitudId(solicitud.getId());
        tarjeta.setActiva(true);
        tarjeta.setFechaAlta(LocalDateTime.now());

        tarjetaService.create(tarjeta);
        log.info("Tarjeta creada para matrícula: {}", tarjeta.getMatricula());
    }

    /**
     * Suma una unidad a la tarjeta duplicada: el socio pasa a tener una tarjeta física más
     * de la misma matrícula y petrolera.
     */
    private void incrementarCantidadTarjeta(SolicitudTarjeta solicitud) {
        if (solicitud.getTarjetaId() == null) {
            throw new RuntimeException("La solicitud de DUPLICADO debe tener una tarjeta asociada");
        }

        Tarjeta tarjeta = tarjetaService.findById(solicitud.getTarjetaId());
        Integer cantidadActual = tarjeta.getCantidad() != null ? tarjeta.getCantidad() : 1;
        tarjeta.setCantidad(cantidadActual + 1);
        tarjetaService.update(tarjeta.getId(), tarjeta);

        log.info("Tarjeta {} duplicada al entregarla. Nueva cantidad: {}", tarjeta.getId(), tarjeta.getCantidad());
    }

    private SocioDTO obtenerSocio(Long socioId) {
        try {
            String url = sociosServiceUrl + "/api/socios/" + socioId;
            log.info("Obteniendo datos del socio desde: {}", url);
            return restTemplate.getForObject(url, SocioDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener datos del socio {}: {}", socioId, e.getMessage());
            throw new RuntimeException("Error al obtener datos del socio", e);
        }
    }

    private PetroleraDTO obtenerPetrolera(Long petroleraId) {
        try {
            String url = petrolerasServiceUrl + "/api/petroleras/" + petroleraId;
            log.info("Obteniendo datos de la petrolera desde: {}", url);
            return restTemplate.getForObject(url, PetroleraDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener datos de la petrolera {}: {}", petroleraId, e.getMessage());
            throw new RuntimeException("Error al obtener datos de la petrolera", e);
        }
    }

    private Map<String, String> crearMapaVariables(SocioDTO socio, PetroleraDTO petrolera, SolicitudTarjeta solicitud) {
        Map<String, String> variables = new HashMap<>();

        // Variables del socio
        variables.put("nombre", socio != null ? socio.getNombre() : "");
        variables.put("nif", socio != null ? socio.getNumeroSocio() : "");
        variables.put("email", socio != null ? socio.getEmail() : "");
        variables.put("telefono", socio != null ? socio.getTelefono() : "");
        variables.put("direccion", socio != null ? socio.getDireccion() : "");
        variables.put("poblacion", socio != null ? socio.getPoblacion() : "");
        variables.put("codigoPostal", socio != null ? socio.getCodigoPostal() : "");
        variables.put("provincia", socio != null ? socio.getProvincia() : "");
        variables.put("direccionCompleta", construirDireccionCompleta(socio));

        // Variables de la petrolera
        variables.put("nombrePetrolera", petrolera != null ? petrolera.getNombre() : "");
        variables.put("emailPetrolera", petrolera != null ? petrolera.getEmail() : "");

        // Variables de la solicitud
        variables.put("matricula", solicitud.getMatricula());
        variables.put("numeroContrato", solicitud.getNumeroContrato() != null ? solicitud.getNumeroContrato() : "");
        variables.put("fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        // Solo un DUPLICADO tiene motivo; en el resto de tipos la plantilla lo resuelve a vacío
        variables.put("motivoDuplicado",
                solicitud.getMotivoDuplicado() != null ? solicitud.getMotivoDuplicado().getEtiqueta() : "");

        return variables;
    }

    /**
     * Dirección postal en una sola línea: "direccion, codigoPostal poblacion (provincia)",
     * omitiendo las partes vacías.
     */
    private String construirDireccionCompleta(SocioDTO socio) {
        if (socio == null) {
            return "";
        }

        List<String> partes = new java.util.ArrayList<>();

        if (tieneValor(socio.getDireccion())) {
            partes.add(socio.getDireccion().trim());
        }

        // Código postal y población van juntos: "28001 Madrid"
        String cpPoblacion = (tieneValor(socio.getCodigoPostal()) ? socio.getCodigoPostal().trim() + " " : "")
                + (tieneValor(socio.getPoblacion()) ? socio.getPoblacion().trim() : "");
        if (!cpPoblacion.isBlank()) {
            partes.add(cpPoblacion.trim());
        }

        String direccionCompleta = String.join(", ", partes);

        // La provincia se añade entre paréntesis, sin coma previa
        if (tieneValor(socio.getProvincia())) {
            String provincia = "(" + socio.getProvincia().trim() + ")";
            direccionCompleta = direccionCompleta.isBlank() ? provincia : direccionCompleta + " " + provincia;
        }

        return direccionCompleta;
    }

    private boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private SolicitudTarjetaDTO convertToDTO(SolicitudTarjeta entity) {
        SolicitudTarjetaDTO dto = new SolicitudTarjetaDTO();
        dto.setId(entity.getId());
        dto.setNumeroSolicitud(entity.getNumeroSolicitud());
        dto.setSocioId(entity.getSocioId());
        dto.setPetroleraId(entity.getPetroleraId());
        dto.setMatricula(entity.getMatricula());
        dto.setNumeroContrato(entity.getNumeroContrato());
        dto.setTipo(entity.getTipo());
        dto.setEstado(entity.getEstado());
        dto.setFechaSolicitud(entity.getFechaSolicitud());
        dto.setFechaProcesado(entity.getFechaProcesado());
        dto.setObservaciones(entity.getObservaciones());
        dto.setProcesadoPor(entity.getProcesadoPor());
        dto.setSolicitadoPor(entity.getSolicitadoPor());
        dto.setTarjetaId(entity.getTarjetaId());
        dto.setMotivoDuplicado(entity.getMotivoDuplicado());
        dto.setFechaLlegadaEstimada(entity.getFechaLlegadaEstimada());
        dto.setFechaEntrega(entity.getFechaEntrega());
        dto.setCorreosEnviados(entity.getCorreosEnviados());
        dto.setRutaPdfEditable(entity.getRutaPdfEditable());
        dto.setNombrePdfEditable(entity.getNombrePdfEditable());
        dto.setRutaPdfEnviado(entity.getRutaPdfEnviado());
        dto.setNombrePdfEnviado(entity.getNombrePdfEnviado());
        dto.setRutaPdfFirmado(entity.getRutaPdfFirmado());
        dto.setNombrePdfFirmado(entity.getNombrePdfFirmado());
        dto.setRutaPdfFinal(entity.getRutaPdfFinal());
        dto.setNombrePdfFinal(entity.getNombrePdfFinal());
        dto.setFechaEnvioSocio(entity.getFechaEnvioSocio());
        dto.setFechaRecepcionFirmado(entity.getFechaRecepcionFirmado());
        dto.setFechaEnvioPetrolera(entity.getFechaEnvioPetrolera());
        dto.setMotivoRechazo(entity.getMotivoRechazo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
