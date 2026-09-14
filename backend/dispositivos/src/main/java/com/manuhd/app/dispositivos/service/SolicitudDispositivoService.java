package com.manuhd.app.dispositivos.service;

import com.manuhd.app.dispositivos.dto.CrearSolicitudDTO;
import com.manuhd.app.dispositivos.dto.EnvioCorreoResult;
import com.manuhd.app.dispositivos.dto.PetroleraDTO;
import com.manuhd.app.dispositivos.dto.SolicitudDispositivoDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
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

        SolicitudDispositivo guardada = solicitudRepository.save(solicitud);
        log.info("Solicitud de dispositivo creada con ID: {}", guardada.getId());

        // Notificar al socio de que su tramite ha sido registrado
        try {
            enviarNotificacionSocioEtapa(guardada, "NOTIF_SOCIO_DISP_CREADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre creacion: {}", e.getMessage());
        }

        // Si no esta programado, enviar inmediatamente
        if (!Boolean.TRUE.equals(dto.getProgramadoEnvio())) {
            enviarAPetrolera(guardada.getId());
        }

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

    @Transactional
    public SolicitudDispositivoDTO enviarAPetrolera(Long solicitudId) {
        SolicitudDispositivo solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("La solicitud no esta en estado PENDIENTE");
        }

        // Obtener datos
        Map<String, Object> socio = obtenerDatosSocio(solicitud.getSocioId());
        Map<String, Object> petrolera = obtenerDatosPetrolera(solicitud.getPetroleraId());
        Map<String, Object> empresa = solicitud.getEmpresaId() != null ?
            obtenerDatosEmpresa(solicitud.getEmpresaId()) : null;

        String emailPetrolera = (String) petrolera.get("email");
        if (emailPetrolera == null || emailPetrolera.isEmpty()) {
            log.warn("La petrolera no tiene email configurado. ID: {}", solicitud.getPetroleraId());
            emailPetrolera = "admin@atg.com";
        }

        // Determinar tipo de plantilla
        String tipoPlantilla = mapearTipoSolicitudAPlantilla(solicitud.getTipoSolicitud());

        // Obtener plantilla de correo
        Map<String, Object> plantilla = obtenerPlantillaCorreo(solicitud.getPetroleraId(), tipoPlantilla);

        // Preparar variables
        Map<String, String> variables = prepararVariablesCorreo(solicitud, socio, empresa, petrolera);

        // Asunto y cuerpo
        String asunto = plantilla != null ? (String) plantilla.get("asunto") :
            "Solicitud de " + getTipoSolicitudLabel(solicitud.getTipoSolicitud()) + " - " + socio.get("nombre");
        String cuerpo = plantilla != null ? (String) plantilla.get("cuerpo") :
            generarCuerpoCorreoDefault(solicitud, socio, empresa);

        try {
            emailService.enviarCorreoConPlantilla(emailPetrolera, asunto, cuerpo, variables);
            registrarEnvioCorreo(solicitud, "ENVIO_PETROLERA", emailPetrolera, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(solicitud, "ENVIO_PETROLERA", emailPetrolera, false, e.getMessage());
            log.error("Error enviando correo a petrolera: {}", e.getMessage());
        }

        // Actualizar estado
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());
        SolicitudDispositivo actualizada = solicitudRepository.save(solicitud);

        log.info("Solicitud {} enviada a petrolera", solicitudId);

        // Notificar al socio
        try {
            enviarNotificacionSocioEtapa(actualizada, "NOTIF_SOCIO_DISP_ENVIADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre envio: {}", e.getMessage());
        }

        return convertirADTO(actualizada);
    }

    @Transactional
    public SolicitudDispositivoDTO responderPetrolera(Long solicitudId, boolean aprobado, String respuesta) {
        SolicitudDispositivo solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("La solicitud no esta en estado ENVIADO_PETROLERA");
        }

        solicitud.setEstado(aprobado ? EstadoSolicitud.APROBADO : EstadoSolicitud.DENEGADO);
        solicitud.setRespuestaPetrolera(respuesta);
        solicitud.setFechaRespuestaPetrolera(LocalDateTime.now());

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
            solicitud.getMonto() != null ? "<li>Monto: " + solicitud.getMonto().toString() + " &euro;</li>" : "",
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

    private String mapearTipoSolicitudAPlantilla(TipoSolicitud tipo) {
        switch (tipo) {
            case ALTA_DISPOSITIVO: return "ALTA_DISPOSITIVO";
            case SOLICITUD_CREDITO: return "SOLICITUD_CREDITO_DISPOSITIVO";
            case BAJA_DISPOSITIVO: return "BAJA_DISPOSITIVO";
            case CAMBIO_MATRICULA: return "CAMBIO_MATRICULA";
            default: return tipo.name();
        }
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
        variables.put("observaciones", solicitud.getObservaciones() != null ? solicitud.getObservaciones() : "");

        return variables;
    }

    private String generarCuerpoCorreoDefault(SolicitudDispositivo solicitud,
            Map<String, Object> socio, Map<String, Object> empresa) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>Solicitud de ").append(getTipoSolicitudLabel(solicitud.getTipoSolicitud())).append("</h2>");
        sb.append("<p><strong>Socio:</strong> ").append(socio.get("nombre")).append("</p>");
        if (empresa != null) {
            sb.append("<p><strong>Empresa:</strong> ").append(empresa.get("nombre")).append("</p>");
        }
        sb.append("<p><strong>Matricula:</strong> ").append(solicitud.getMatricula() != null ? solicitud.getMatricula() : "N/A").append("</p>");
        if (solicitud.getMatriculaDestino() != null) {
            sb.append("<p><strong>Matricula Destino:</strong> ").append(solicitud.getMatriculaDestino()).append("</p>");
        }
        if (solicitud.getMonto() != null) {
            sb.append("<p><strong>Monto:</strong> ").append(solicitud.getMonto()).append(" &euro;</p>");
        }
        if (solicitud.getObservaciones() != null && !solicitud.getObservaciones().isEmpty()) {
            sb.append("<p><strong>Observaciones:</strong> ").append(solicitud.getObservaciones()).append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
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
        dto.setSocioId(solicitud.getSocioId());
        dto.setEmpresaId(solicitud.getEmpresaId());
        dto.setPetroleraId(solicitud.getPetroleraId());
        dto.setDispositivoId(solicitud.getDispositivoId());
        dto.setTipoSolicitud(solicitud.getTipoSolicitud());
        dto.setEstado(solicitud.getEstado());
        dto.setMatricula(solicitud.getMatricula());
        dto.setMatriculaDestino(solicitud.getMatriculaDestino());
        dto.setMonto(solicitud.getMonto());
        dto.setObservaciones(solicitud.getObservaciones());
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
