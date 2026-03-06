package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.dto.*;
import com.manuhd.app.tarjetas.model.*;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setObservaciones(dto.getObservaciones());
        solicitud.setSolicitadoPor(dto.getSolicitadoPor());
        solicitud.setTarjetaId(dto.getTarjetaId());
        solicitud.setFechaSolicitud(LocalDateTime.now());

        // Para LLEGADA, si viene con fecha estimada, la registramos
        if (dto.getTipo() == TipoSolicitud.LLEGADA && dto.getFechaLlegadaEstimada() != null) {
            solicitud.setFechaLlegadaEstimada(dto.getFechaLlegadaEstimada());
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

    @Transactional
    public SolicitudTarjetaDTO completar(Long id, String procesadoPor) {
        log.info("Completando solicitud con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() == EstadoSolicitud.COMPLETADA) {
            throw new RuntimeException("La solicitud ya está completada");
        }

        solicitud.setEstado(EstadoSolicitud.COMPLETADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(procesadoPor);

        // Ejecutar acción según el tipo
        ejecutarAccionCompletado(solicitud);

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud completada con id: {}", updated.getId());

        return convertToDTO(updated);
    }

    @Transactional
    public SolicitudTarjetaDTO rechazar(Long id, String motivo, String procesadoPor) {
        log.info("Rechazando solicitud con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se pueden rechazar solicitudes pendientes");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(procesadoPor);
        solicitud.setObservaciones(motivo);

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud rechazada con id: {}", updated.getId());

        return convertToDTO(updated);
    }

    @Transactional
    public SolicitudTarjetaDTO aprobar(Long id, String procesadoPor) {
        log.info("Aprobando solicitud con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar solicitudes pendientes");
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaProcesado(LocalDateTime.now());
        solicitud.setProcesadoPor(procesadoPor);

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud aprobada con id: {}", updated.getId());

        // Enviar correo de aprobación (si existe la plantilla)
        try {
            SocioDTO socio = obtenerSocio(solicitud.getSocioId());
            PetroleraDTO petrolera = obtenerPetrolera(solicitud.getPetroleraId());
            Map<String, String> variables = crearMapaVariables(socio, petrolera, solicitud);

            // TODO: Implementar envío de correo ALTA_APROBADA cuando exista la plantilla
        } catch (Exception e) {
            log.error("Error al enviar correo de aprobación: {}", e.getMessage());
        }

        return convertToDTO(updated);
    }

    @Transactional
    public SolicitudTarjetaDTO aprobarBaja(Long id, AprobarBajaDTO dto) {
        log.info("Aprobando solicitud de BAJA con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar solicitudes pendientes");
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
        solicitud.setProcesadoPor(dto.getProcesadoPor());
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

            PlantillaTarjeta plantilla = plantillaService.obtenerPlantillaActiva(TipoPlantilla.BAJA_SOCIO);
            if (plantilla != null) {
                EnvioCorreoResult resultado = emailService.enviarCorreoConPlantilla(
                        socio.getEmail(),
                        plantilla.getAsunto(),
                        plantilla.getCuerpo(),
                        variables,
                        TipoPlantilla.BAJA_SOCIO.name()
                );

                // Registrar correo enviado
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.toString());
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

    @Transactional
    public SolicitudTarjetaDTO aprobarDuplicado(Long id, AprobarDuplicadoDTO dto) {
        log.info("Aprobando solicitud de DUPLICADO con id: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar solicitudes pendientes");
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
        solicitud.setProcesadoPor(dto.getProcesadoPor());
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

            PlantillaTarjeta plantilla = plantillaService.obtenerPlantillaActiva(TipoPlantilla.DUPLICADO_SOCIO);
            if (plantilla != null) {
                EnvioCorreoResult resultado = emailService.enviarCorreoConPlantilla(
                        socio.getEmail(),
                        plantilla.getAsunto(),
                        plantilla.getCuerpo(),
                        variables,
                        TipoPlantilla.DUPLICADO_SOCIO.name()
                );

                // Registrar correo enviado
                if (correosEnviados.length() > 0) correosEnviados.append("\n");
                correosEnviados.append(resultado.toString());
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

    @Transactional
    public SolicitudTarjetaDTO registrarLlegada(Long id, RegistrarLlegadaDTO dto) {
        log.info("Registrando llegada de tarjeta para solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new RuntimeException("Solo se puede registrar llegada de solicitudes aprobadas");
        }

        solicitud.setEstado(EstadoSolicitud.TARJETA_LLEGADA);
        solicitud.setFechaLlegadaEstimada(dto.getFechaLlegadaEstimada());
        solicitud.setNumeroContrato(dto.getNumeroContrato());  // Actualizar numeroContrato si se proporciona
        solicitud.setProcesadoPor(dto.getProcesadoPor());
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

    @Transactional
    public SolicitudTarjetaDTO marcarEntregada(Long id, MarcarEntregadaDTO dto) {
        log.info("Marcando como entregada la solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.TARJETA_LLEGADA) {
            throw new RuntimeException("Solo se puede marcar como entregada si la tarjeta ha llegado");
        }

        solicitud.setEstado(EstadoSolicitud.ENTREGADA);
        solicitud.setFechaEntrega(dto.getFechaEntrega() != null ? dto.getFechaEntrega() : LocalDateTime.now());
        solicitud.setProcesadoPor(dto.getProcesadoPor());
        if (dto.getObservaciones() != null) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        // Crear la tarjeta activa
        if (solicitud.getTipo() == TipoSolicitud.ALTA || solicitud.getTipo() == TipoSolicitud.DUPLICADO) {
            crearTarjeta(solicitud);
        }

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud marcada como entregada: {}", updated.getId());

        return convertToDTO(updated);
    }

    @Transactional
    public SolicitudTarjetaDTO finalizar(Long id) {
        log.info("Finalizando solicitud: {}", id);

        SolicitudTarjeta solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));

        if (solicitud.getEstado() != EstadoSolicitud.ENTREGADA) {
            throw new RuntimeException("Solo se puede finalizar una solicitud entregada");
        }

        solicitud.setEstado(EstadoSolicitud.COMPLETADA);

        SolicitudTarjeta updated = repository.save(solicitud);
        log.info("Solicitud finalizada: {}", updated.getId());

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

    private List<EnvioCorreoResult> enviarCorreoLlegada(SocioDTO socio, Map<String, String> variables) {
        List<EnvioCorreoResult> resultados = new java.util.ArrayList<>();
        TipoPlantilla tipoPlantilla;

        if ("Madrid".equalsIgnoreCase(socio.getProvincia())) {
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

    private void ejecutarAccionCompletado(SolicitudTarjeta solicitud) {
        log.info("Ejecutando acción de completado para tipo: {}", solicitud.getTipo());

        switch (solicitud.getTipo()) {
            case ALTA:
                crearTarjeta(solicitud);
                break;
            case BAJA:
                desactivarTarjeta(solicitud);
                break;
            case LLEGADA:
            case DUPLICADO:
                // No requieren acción adicional
                break;
        }
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

    private void desactivarTarjeta(SolicitudTarjeta solicitud) {
        log.info("Desactivando tarjeta con matrícula: {}", solicitud.getMatricula());

        List<Tarjeta> tarjetas = tarjetaService.findByMatricula(solicitud.getMatricula());
        if (tarjetas.isEmpty()) {
            log.warn("No se encontraron tarjetas con matrícula: {}", solicitud.getMatricula());
            return;
        }

        for (Tarjeta tarjeta : tarjetas) {
            if (tarjeta.getActiva() && tarjeta.getSocioId().equals(solicitud.getSocioId())) {
                tarjeta.setActiva(false);
                tarjetaService.update(tarjeta.getId(), tarjeta);
                log.info("Tarjeta desactivada: {}", tarjeta.getId());
            }
        }
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
        variables.put("provincia", socio != null ? socio.getProvincia() : "");

        // Variables de la petrolera
        variables.put("nombrePetrolera", petrolera != null ? petrolera.getNombre() : "");
        variables.put("emailPetrolera", petrolera != null ? petrolera.getEmail() : "");

        // Variables de la solicitud
        variables.put("matricula", solicitud.getMatricula());
        variables.put("numeroContrato", "");
        variables.put("fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        return variables;
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
