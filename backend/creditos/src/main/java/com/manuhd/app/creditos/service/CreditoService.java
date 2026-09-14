package com.manuhd.app.creditos.service;

import com.manuhd.app.creditos.dto.CreditoDTO;
import com.manuhd.app.creditos.dto.CrearCreditoDTO;
import com.manuhd.app.creditos.dto.EnvioCorreoResult;
import com.manuhd.app.creditos.dto.PetroleraDTO;
import com.manuhd.app.creditos.model.Credito;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.model.TipoCredito;
import com.manuhd.app.creditos.repository.CreditoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class CreditoService {

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${microservices.socios.url:http://localhost:8081}")
    private String sociosBaseUrl;

    @Value("${microservices.petroleras.url:http://localhost:8082}")
    private String petrolerasBaseUrl;

    @Transactional(readOnly = true)
    public List<CreditoDTO> listarTodos() {
        return creditoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CreditoDTO obtenerPorId(Long id) {
        Credito credito = creditoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado con id: " + id));
        return convertirADTO(credito);
    }

    @Transactional(readOnly = true)
    public List<CreditoDTO> listarPorSocio(Long socioId) {
        return creditoRepository.findBySocioId(socioId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditoDTO> listarPorPetrolera(Long petroleraId) {
        return creditoRepository.findByPetroleraId(petroleraId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditoDTO> listarPorEstado(EstadoCredito estado) {
        return creditoRepository.findByEstado(estado).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CreditoDTO crear(CrearCreditoDTO dto) {
        // La petrolera debe operar con créditos según el procedimiento de ATG
        validarPetroleraOperaCreditos(dto.getPetroleraId());

        // Validación condicional del monto: obligatorio excepto para DEVOLUCION_AVAL
        if (dto.getTipoCredito() != TipoCredito.DEVOLUCION_AVAL) {
            if (dto.getMonto() != null && dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El monto debe ser mayor que 0");
            }
        }

        Credito credito = new Credito();
        credito.setSocioId(dto.getSocioId());
        credito.setEmpresaId(dto.getEmpresaId());
        credito.setPetroleraId(dto.getPetroleraId());
        credito.setTipoCredito(dto.getTipoCredito());
        credito.setMonto(dto.getTipoCredito() == TipoCredito.DEVOLUCION_AVAL ? null : dto.getMonto());
        credito.setObservaciones(dto.getObservaciones());
        credito.setEstado(EstadoCredito.PENDIENTE);
        credito.setProgramadoEnvio(Boolean.TRUE.equals(dto.getProgramadoEnvio()));
        credito.setFechaProgramadaEnvio(dto.getFechaProgramadaEnvio());

        Credito guardado = creditoRepository.save(credito);
        log.info("Crédito creado con ID: {}", guardado.getId());

        // Notificar al socio de que su trámite ha sido registrado
        try {
            enviarNotificacionSocioEtapa(guardado, "NOTIF_SOCIO_CREADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre creación: {}", e.getMessage());
        }

        // Si no está programado, enviar inmediatamente
        if (!Boolean.TRUE.equals(dto.getProgramadoEnvio())) {
            enviarAPetrolera(guardado.getId());
        }

        return convertirADTO(guardado);
    }

    @Transactional
    public CreditoDTO enviarAPetrolera(Long creditoId) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado con id: " + creditoId));

        if (credito.getEstado() != EstadoCredito.PENDIENTE) {
            throw new RuntimeException("El crédito no está en estado PENDIENTE");
        }

        // Obtener datos de socio, empresa y petrolera
        Map<String, Object> socio = obtenerDatosSocio(credito.getSocioId());
        Map<String, Object> petrolera = obtenerDatosPetrolera(credito.getPetroleraId());
        Map<String, Object> empresa = credito.getEmpresaId() != null ?
            obtenerDatosEmpresa(credito.getEmpresaId()) : null;

        String emailPetrolera = (String) petrolera.get("email");
        if (emailPetrolera == null || emailPetrolera.isEmpty()) {
            log.warn("La petrolera no tiene email configurado. ID: {}", credito.getPetroleraId());
            emailPetrolera = "admin@atg.com"; // Email de fallback para simulación
        }

        // Obtener plantilla de correo
        String tipoPlantilla = credito.getTipoCredito().name();
        Map<String, Object> plantilla = obtenerPlantillaCorreo(credito.getPetroleraId(), tipoPlantilla);

        // Preparar variables para la plantilla
        Map<String, String> variables = prepararVariablesCorreo(credito, socio, empresa, petrolera);

        // Enviar correo a la petrolera
        String asunto = plantilla != null ? (String) plantilla.get("asunto") :
            "Solicitud de " + credito.getTipoCredito();
        String cuerpo = plantilla != null ? (String) plantilla.get("cuerpo") :
            generarCuerpoCorreoDefault(credito, socio, empresa);

        try {
            emailService.enviarCorreoConPlantilla(emailPetrolera, asunto, cuerpo, variables);
            registrarEnvioCorreo(credito, "ENVIO_PETROLERA", emailPetrolera, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(credito, "ENVIO_PETROLERA", emailPetrolera, false, e.getMessage());
            log.error("Error enviando correo a petrolera: {}", e.getMessage());
        }

        // Actualizar estado
        credito.setEstado(EstadoCredito.ENVIADO_PETROLERA);
        credito.setFechaEnvioPetrolera(LocalDateTime.now());
        Credito actualizado = creditoRepository.save(credito);

        log.info("Crédito {} enviado a petrolera", creditoId);

        // Notificar al socio de que se ha enviado a la petrolera
        try {
            enviarNotificacionSocioEtapa(actualizado, "NOTIF_SOCIO_ENVIADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre envío: {}", e.getMessage());
        }

        return convertirADTO(actualizado);
    }

    @Transactional
    public CreditoDTO responderPetrolera(Long creditoId, boolean aprobado, String respuesta,
            BigDecimal montoConcedido) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado con id: " + creditoId));

        if (credito.getEstado() != EstadoCredito.ENVIADO_PETROLERA) {
            throw new RuntimeException("El crédito no está en estado ENVIADO_PETROLERA");
        }

        // El importe concedido solo tiene sentido al aprobar. Al denegar se ignora lo que llegue
        // y se deja a null: no hay importe concedido si la petrolera no concede nada.
        if (aprobado) {
            credito.setMontoConcedido(validarMontoConcedido(credito, montoConcedido));
        } else {
            credito.setMontoConcedido(null);
        }

        credito.setEstado(aprobado ? EstadoCredito.APROBADO : EstadoCredito.DENEGADO);
        credito.setRespuestaPetrolera(respuesta);
        credito.setFechaRespuestaPetrolera(LocalDateTime.now());

        Credito actualizado = creditoRepository.save(credito);
        log.info("Crédito {} respondido por petrolera: {}", creditoId, aprobado ? "APROBADO" : "DENEGADO");

        // Notificar al socio automáticamente
        notificarSocio(creditoId);

        return convertirADTO(actualizado);
    }

    /**
     * Valida el importe concedido al aprobar un crédito.
     *
     * La devolución de aval no lleva importe solicitado, por lo que tampoco lleva concedido:
     * en ese caso se ignora lo que llegue y se guarda null.
     */
    private BigDecimal validarMontoConcedido(Credito credito, BigDecimal montoConcedido) {
        if (credito.getTipoCredito() == TipoCredito.DEVOLUCION_AVAL) {
            return null;
        }
        if (montoConcedido == null || montoConcedido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El importe concedido es obligatorio y debe ser mayor que 0");
        }
        return montoConcedido;
    }

    @Transactional
    public CreditoDTO notificarSocio(Long creditoId) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado con id: " + creditoId));

        if (credito.getEstado() != EstadoCredito.APROBADO && credito.getEstado() != EstadoCredito.DENEGADO) {
            throw new RuntimeException("El crédito no tiene respuesta de la petrolera");
        }

        // Obtener datos del socio
        Map<String, Object> socio = obtenerDatosSocio(credito.getSocioId());
        String emailSocio = (String) socio.get("email");

        if (emailSocio == null || emailSocio.isEmpty()) {
            log.warn("El socio no tiene email configurado. ID: {}", credito.getSocioId());
            emailSocio = "socio@example.com"; // Email de fallback para simulación
        }

        // Preparar correo para el socio
        String asunto = String.format("Respuesta sobre su %s",
            credito.getTipoCredito().name().replace("_", " ").toLowerCase());

        String cuerpo = String.format(
            "<html><body>" +
            "<h2>Estimado/a %s</h2>" +
            "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido <strong>%s</strong> por la petrolera.</p>" +
            "<p><strong>Detalles:</strong></p>" +
            "<ul>" +
            "<li>Tipo: %s</li>" +
            "<li>Importe solicitado: %s</li>" +
            "%s" +
            "<li>Estado: %s</li>" +
            "<li>Respuesta: %s</li>" +
            "</ul>" +
            "<p>Para más información, contacte con nosotros.</p>" +
            "<p>Saludos cordiales,<br>Equipo ATG</p>" +
            "</body></html>",
            socio.get("nombre"),
            credito.getTipoCredito().name().replace("_", " "),
            credito.getEstado() == EstadoCredito.APROBADO ? "APROBADA" : "DENEGADA",
            credito.getTipoCredito(),
            credito.getMonto() != null ? credito.getMonto().toString() + " €" : "N/A",
            credito.getMontoConcedido() != null
                ? "<li>Importe concedido: " + credito.getMontoConcedido().toString() + " €</li>"
                : "",
            credito.getEstado(),
            credito.getRespuestaPetrolera() != null ? credito.getRespuestaPetrolera() : "Sin comentarios"
        );

        try {
            emailService.enviarCorreoHTML(emailSocio, asunto, cuerpo);
            registrarEnvioCorreo(credito, "NOTIF_SOCIO_RESULTADO", emailSocio, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(credito, "NOTIF_SOCIO_RESULTADO", emailSocio, false, e.getMessage());
            log.error("Error notificando socio resultado: {}", e.getMessage());
        }

        credito.setEstado(credito.getEstado() == EstadoCredito.APROBADO
            ? EstadoCredito.COMPLETADO_APROBADO
            : EstadoCredito.COMPLETADO_DENEGADO);
        credito.setFechaNotificacionSocio(LocalDateTime.now());
        Credito actualizado = creditoRepository.save(credito);

        log.info("Socio notificado sobre crédito {}", creditoId);

        return convertirADTO(actualizado);
    }

    private Map<String, String> prepararVariablesCorreo(Credito credito,
            Map<String, Object> socio, Map<String, Object> empresa, Map<String, Object> petrolera) {
        Map<String, String> variables = new HashMap<>();

        variables.put("socio_nombre", (String) socio.get("nombre"));
        variables.put("socio_email", (String) socio.get("email"));
        variables.put("socio_telefono", (String) socio.get("telefono"));
        variables.put("socio_numero", (String) socio.get("numeroSocio"));

        if (empresa != null) {
            variables.put("empresa_nombre", (String) empresa.get("nombre"));
            variables.put("empresa_cif", (String) empresa.get("cif"));
            variables.put("empresa_email", (String) empresa.get("email"));
        }

        variables.put("petrolera_nombre", (String) petrolera.get("nombre"));
        variables.put("tipo_credito", credito.getTipoCredito().name());
        variables.put("monto", credito.getMonto() != null ? credito.getMonto().toString() : "N/A");
        variables.put("monto_concedido",
                credito.getMontoConcedido() != null ? credito.getMontoConcedido().toString() : "N/A");
        variables.put("observaciones", credito.getObservaciones() != null ? credito.getObservaciones() : "");
        variables.put("fecha_solicitud", credito.getCreatedAt().toString());

        return variables;
    }

    private String generarCuerpoCorreoDefault(Credito credito, Map<String, Object> socio, Map<String, Object> empresa) {
        return String.format(
            "<html><body>" +
            "<h2>Solicitud de %s</h2>" +
            "<p><strong>Socio:</strong> %s</p>" +
            "<p><strong>Empresa:</strong> %s</p>" +
            "<p><strong>Monto:</strong> %s</p>" +
            "<p><strong>Observaciones:</strong> %s</p>" +
            "</body></html>",
            credito.getTipoCredito().name().replace("_", " "),
            socio.get("nombre"),
            empresa != null ? empresa.get("nombre") : "N/A",
            credito.getMonto() != null ? credito.getMonto().toString() + " €" : "N/A",
            credito.getObservaciones() != null ? credito.getObservaciones() : "Sin observaciones"
        );
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

    /**
     * Comprueba que la petrolera seleccionada opera con créditos.
     *
     * Un flag a null significa "sin restricción configurada" y por tanto se permite: las
     * petroleras que ya existían en base de datos no tienen valor y deben seguir funcionando
     * igual hasta que un administrador marque la restricción.
     */
    private void validarPetroleraOperaCreditos(Long petroleraId) {
        PetroleraDTO petrolera;
        try {
            petrolera = restTemplate.getForObject(
                    petrolerasBaseUrl + "/api/petroleras/" + petroleraId, PetroleraDTO.class);
        } catch (Exception e) {
            log.error("Error al validar la petrolera {}: {}", petroleraId, e.getMessage());
            throw new RuntimeException(
                    "No se ha podido verificar la petrolera seleccionada. Inténtelo de nuevo más tarde.");
        }

        if (petrolera == null) {
            throw new RuntimeException("Petrolera no encontrada con id: " + petroleraId);
        }

        // null = sin restricción => permitido
        Boolean operaCreditos = petrolera.getOperaCreditos();
        if (operaCreditos != null && !operaCreditos) {
            String nombre = petrolera.getNombre() != null ? petrolera.getNombre() : "seleccionada";
            throw new RuntimeException("La petrolera " + nombre + " no opera con créditos");
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
            log.warn("No se encontró plantilla de correo para petrolera {} tipo {}", petroleraId, tipoPlantilla);
            return null;
        }
    }

    private CreditoDTO convertirADTO(Credito credito) {
        CreditoDTO dto = new CreditoDTO();
        dto.setId(credito.getId());
        dto.setSocioId(credito.getSocioId());
        dto.setEmpresaId(credito.getEmpresaId());
        dto.setPetroleraId(credito.getPetroleraId());
        dto.setTipoCredito(credito.getTipoCredito());
        dto.setEstado(credito.getEstado());
        dto.setMonto(credito.getMonto());
        dto.setMontoConcedido(credito.getMontoConcedido());
        dto.setObservaciones(credito.getObservaciones());
        dto.setFechaEnvioPetrolera(credito.getFechaEnvioPetrolera());
        dto.setFechaRespuestaPetrolera(credito.getFechaRespuestaPetrolera());
        dto.setFechaNotificacionSocio(credito.getFechaNotificacionSocio());
        dto.setRespuestaPetrolera(credito.getRespuestaPetrolera());
        dto.setProgramadoEnvio(credito.getProgramadoEnvio());
        dto.setFechaProgramadaEnvio(credito.getFechaProgramadaEnvio());
        dto.setCorreosEnviados(credito.getCorreosEnviados());
        dto.setCreatedAt(credito.getCreatedAt());
        dto.setUpdatedAt(credito.getUpdatedAt());

        // Obtener datos adicionales de otros servicios
        try {
            Map<String, Object> socio = obtenerDatosSocio(credito.getSocioId());
            dto.setSocioNombre((String) socio.get("nombre"));
            dto.setSocioEmail((String) socio.get("email"));

            if (credito.getEmpresaId() != null) {
                Map<String, Object> empresa = obtenerDatosEmpresa(credito.getEmpresaId());
                dto.setEmpresaNombre((String) empresa.get("nombre"));
                dto.setEmpresaEmail((String) empresa.get("email"));
            }

            Map<String, Object> petrolera = obtenerDatosPetrolera(credito.getPetroleraId());
            dto.setPetroleraNombre((String) petrolera.get("nombre"));
            dto.setPetroleraEmail((String) petrolera.get("email"));
        } catch (Exception e) {
            log.warn("Error al obtener datos relacionados: {}", e.getMessage());
        }

        return dto;
    }

    private void registrarEnvioCorreo(Credito credito, String tipoPlantilla, String destinatario, boolean exito, String errorMsg) {
        EnvioCorreoResult resultado = exito
                ? new EnvioCorreoResult(true, tipoPlantilla, destinatario)
                : new EnvioCorreoResult(false, tipoPlantilla, destinatario, errorMsg);

        StringBuilder sb = new StringBuilder(
                credito.getCorreosEnviados() != null ? credito.getCorreosEnviados() : ""
        );
        if (sb.length() > 0) sb.append("\n");
        sb.append(resultado.toString());
        credito.setCorreosEnviados(sb.toString());
        creditoRepository.save(credito);
    }

    private void enviarNotificacionSocioEtapa(Credito credito, String tipoNotificacion) {
        Map<String, Object> socio = obtenerDatosSocio(credito.getSocioId());
        Map<String, Object> petrolera = obtenerDatosPetrolera(credito.getPetroleraId());
        String emailSocio = (String) socio.get("email");
        if (emailSocio == null || emailSocio.isEmpty()) {
            emailSocio = "socio@example.com";
        }

        String nombreSocio = (String) socio.get("nombre");
        String nombrePetrolera = (String) petrolera.get("nombre");
        String tipoCredito = credito.getTipoCredito().name().replace("_", " ").toLowerCase();
        String asunto;
        String cuerpo;

        switch (tipoNotificacion) {
            case "NOTIF_SOCIO_CREADO":
                asunto = "Su solicitud de " + tipoCredito + " ha sido registrada";
                cuerpo = String.format(
                    "<html><body>" +
                    "<h2>Estimado/a %s</h2>" +
                    "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido registrada correctamente.</p>" +
                    "<p>Procederemos a enviarla a <strong>%s</strong> para su tramitación.</p>" +
                    "<p>Le mantendremos informado/a del estado de su solicitud.</p>" +
                    "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                    "</body></html>",
                    nombreSocio, tipoCredito, nombrePetrolera);
                break;
            case "NOTIF_SOCIO_ENVIADO":
                asunto = "Su solicitud de " + tipoCredito + " ha sido enviada a la petrolera";
                cuerpo = String.format(
                    "<html><body>" +
                    "<h2>Estimado/a %s</h2>" +
                    "<p>Le informamos que su solicitud de <strong>%s</strong> ha sido enviada a <strong>%s</strong> para su tramitación.</p>" +
                    "<p>Cuando recibamos respuesta de la petrolera, le notificaremos el resultado.</p>" +
                    "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                    "</body></html>",
                    nombreSocio, tipoCredito, nombrePetrolera);
                break;
            default:
                return;
        }

        try {
            emailService.enviarCorreoHTML(emailSocio, asunto, cuerpo);
            registrarEnvioCorreo(credito, tipoNotificacion, emailSocio, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(credito, tipoNotificacion, emailSocio, false, e.getMessage());
            log.error("Error en notificación {} al socio: {}", tipoNotificacion, e.getMessage());
        }
    }
}
