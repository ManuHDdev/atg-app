package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.dto.*;
import com.manuhd.app.tarjetas.model.*;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import com.manuhd.app.tarjetas.security.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDateTime;
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

    @Value("${app.socios.url:http://localhost:8081}")
    private String sociosServiceUrl;

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
        } else {
            solicitud.setEstado(EstadoSolicitud.PENDIENTE);
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

    /** Registra el duplicado confirmado por la petrolera y cierra la solicitud. */
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

        // Buscar la tarjeta e incrementar la cantidad
        Tarjeta tarjeta = tarjetaService.findById(solicitud.getTarjetaId());

        // Incrementar cantidad (nueva tarjeta física duplicada)
        Integer cantidadActual = tarjeta.getCantidad() != null ? tarjeta.getCantidad() : 1;
        tarjeta.setCantidad(cantidadActual + 1);
        tarjetaService.update(tarjeta.getId(), tarjeta);

        log.info("Tarjeta {} duplicada. Nueva cantidad: {}", tarjeta.getId(), tarjeta.getCantidad());

        // Marcar solicitud como COMPLETADA
        solicitud.setEstado(EstadoSolicitud.COMPLETADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(usuarioActual.nombreUsuario());
        if (dto.getObservaciones() != null && !dto.getObservaciones().isEmpty()) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud de DUPLICADO completada con id: {}", updated.getId());

        // Enviar correo de confirmación al socio
        StringBuilder correosEnviados = new StringBuilder(updated.getCorreosEnviados() != null ? updated.getCorreosEnviados() : "");
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);
            variables.put("fechaRespuesta", dto.getFechaRespuesta().toString());
            variables.put("cantidad", String.valueOf(tarjeta.getCantidad()));

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
     * Paso intermedio del ALTA: la petrolera ya la aprobó y ahora llega la tarjeta física.
     * No aplica a una solicitud de tipo LLEGADA, que nace ya en TARJETA_LLEGADA con su correo
     * enviado; volver a pasar por aquí duplicaría el aviso al socio.
     */
    @Transactional
    public SolicitudTarjetaDTO registrarLlegada(Long id, RegistrarLlegadaDTO dto) {
        log.info("Registrando llegada de tarjeta para solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getTipo() != TipoSolicitud.ALTA) {
            throw new RuntimeException("Solo se puede registrar la llegada de solicitudes de ALTA");
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

        // Crear la tarjeta activa. Solo el ALTA crea una Tarjeta nueva: un DUPLICADO no
        // genera fila propia porque aprobarDuplicado incrementa la cantidad de la tarjeta
        // existente y cierra la solicitud sin pasar por llegada/entrega.
        if (solicitud.getTipo() == TipoSolicitud.ALTA) {
            crearTarjeta(solicitud);
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
                resultados.addAll(enviarCorreoAlta(socio, petrolera, variables));
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

    private List<EnvioCorreoResult> enviarCorreoAlta(SocioDTO socio, PetroleraDTO petrolera, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        log.info("Enviando correos de alta al socio y petrolera");

        // Correo al socio
        PlantillaTarjeta plantillaSocio = plantillaService.obtenerPlantillaActiva(TipoPlantilla.ALTA_SOCIO);
        EnvioCorreoResult resultadoSocio = emailService.enviarCorreoConPlantilla(
                socio.getEmail(),
                plantillaSocio.getAsunto(),
                plantillaSocio.getCuerpo(),
                variables,
                TipoPlantilla.ALTA_SOCIO.name()
        );
        resultados.add(resultadoSocio);

        // Correo a la petrolera
        PlantillaTarjeta plantillaPetrolera = plantillaService.obtenerPlantillaActiva(TipoPlantilla.ALTA_PETROLERA);
        EnvioCorreoResult resultadoPetrolera = emailService.enviarCorreoConPlantilla(
                petrolera.getEmail(),
                plantillaPetrolera.getAsunto(),
                plantillaPetrolera.getCuerpo(),
                variables,
                TipoPlantilla.ALTA_PETROLERA.name()
        );
        resultados.add(resultadoPetrolera);
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
            String url = "http://localhost:8082/api/petroleras/" + petroleraId;
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
        dto.setFechaLlegadaEstimada(entity.getFechaLlegadaEstimada());
        dto.setFechaEntrega(entity.getFechaEntrega());
        dto.setCorreosEnviados(entity.getCorreosEnviados());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
