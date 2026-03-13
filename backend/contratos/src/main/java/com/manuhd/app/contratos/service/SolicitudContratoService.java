package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.client.PetrolerasClient;
import com.manuhd.app.contratos.client.SociosClient;
import com.manuhd.app.contratos.dto.CrearSolicitudDTO;
import com.manuhd.app.contratos.dto.EnvioCorreoResult;
import com.manuhd.app.contratos.dto.FiltroSolicitudesDTO;
import com.manuhd.app.contratos.dto.SolicitudContratoDTO;
import com.manuhd.app.contratos.model.ContratoSocio;
import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.model.SolicitudContrato;
import com.manuhd.app.contratos.model.TipoSolicitudContrato;
import com.manuhd.app.contratos.repository.SolicitudContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudContratoService {

    private final SolicitudContratoRepository solicitudRepository;
    private final PdfService pdfService;
    private final PetrolerasClient petrolerasClient;
    private final ContratoSocioService contratoSocioService;
    private final EmailService emailService;
    private final SociosClient sociosClient;

    @Transactional
    public SolicitudContratoDTO crearSolicitud(CrearSolicitudDTO dto) throws IOException {
        log.info("Creando nueva solicitud para socio: {}, petrolera: {}, tipo: {}",
            dto.getSocioId(), dto.getPetroleraId(), dto.getTipoContratoId());

        String rutaPdfEditable = null;

        // 1. Generar número de solicitud automático
        String numeroSolicitud = generarNumeroSolicitud();

        // 2. Si tiene tipoSolicitudPetroleraId, obtener plantilla del microservicio Petroleras
        if (dto.getTipoSolicitudPetroleraId() != null) {
            try {
                log.info("Obteniendo plantilla PDF del tipo de solicitud ID: {}", dto.getTipoSolicitudPetroleraId());

                // Obtener el PDF desde el microservicio de Petroleras
                byte[] plantillaPdf = petrolerasClient.obtenerPlantillaPdf(dto.getTipoSolicitudPetroleraId());

                // Crear directorio para la solicitud
                Path directorioSolicitud = Paths.get("./storage/contratos/solicitudes", numeroSolicitud);
                Files.createDirectories(directorioSolicitud);

                // Guardar la plantilla original para poder visualizarla siempre
                Path rutaPlantillaOriginal = directorioSolicitud.resolve("plantilla_original.pdf");
                Files.write(rutaPlantillaOriginal, plantillaPdf);
                log.info("Plantilla original guardada en: {}", rutaPlantillaOriginal);

                // Guardar también como editable.pdf (esta puede ser sobrescrita por el usuario)
                Path rutaArchivo = directorioSolicitud.resolve("editable.pdf");
                Files.write(rutaArchivo, plantillaPdf);

                rutaPdfEditable = rutaArchivo.toString();
                log.info("Plantilla PDF copiada a: {}", rutaPdfEditable);

            } catch (IOException e) {
                log.error("Error al obtener plantilla del tipo de solicitud", e);
                throw new RuntimeException("No se pudo obtener la plantilla PDF para el tipo de solicitud seleccionado. " +
                    "Verifique que el tipo de solicitud tenga una plantilla configurada.", e);
            }
        } else {
            // Si no hay tipo de solicitud, solo crear el directorio sin PDF
            Path directorioSolicitud = Paths.get("./storage/contratos/solicitudes", numeroSolicitud);
            Files.createDirectories(directorioSolicitud);
        }

        // 3. Crear registro en BD
        SolicitudContrato solicitud = new SolicitudContrato();
        solicitud.setNumeroSolicitud(numeroSolicitud);
        solicitud.setSocioId(dto.getSocioId());
        solicitud.setEmpresaId(dto.getEmpresaId());
        solicitud.setTarjetaId(dto.getTarjetaId());
        solicitud.setPetroleraId(dto.getPetroleraId());
        solicitud.setTipoContratoId(dto.getTipoContratoId());
        solicitud.setTipoSolicitudPetroleraId(dto.getTipoSolicitudPetroleraId());
        solicitud.setPlantillaId(dto.getTipoSolicitudPetroleraId()); // Usar el ID del tipo de solicitud como referencia
        solicitud.setFechaHoraSolicitud(LocalDateTime.now());
        solicitud.setSolicitadoPor(dto.getSolicitadoPor());
        solicitud.setEsAutonomo(dto.getEsAutonomo());
        solicitud.setObservaciones(dto.getObservaciones());
        solicitud.setTipoSolicitud(dto.getTipoSolicitud());
        solicitud.setContratoId(dto.getContratoId());

        // Resolver subtipoNombre según el tipo de solicitud
        if ((dto.getTipoSolicitud() == TipoSolicitudContrato.BAJA ||
             dto.getTipoSolicitud() == TipoSolicitudContrato.CAMBIO_CONDICIONES)
            && dto.getContratoId() != null) {
            // Para BAJA y CAMBIO_CONDICIONES, heredar del contrato original
            log.info("Heredando subtipo del contrato original ID: {}", dto.getContratoId());
            ContratoSocio contratoOriginal = contratoSocioService.findById(dto.getContratoId());
            if (contratoOriginal.getSubtipoContrato() != null) {
                solicitud.setSubtipoNombre(contratoOriginal.getSubtipoContrato());
                log.info("subtipoNombre heredado: '{}'", contratoOriginal.getSubtipoContrato());
            }
        }
        // Si no se heredó, resolver desde el microservicio de petroleras
        if (solicitud.getSubtipoNombre() == null && dto.getTipoSolicitudPetroleraId() != null) {
            log.info("Resolviendo subtipo desde petroleras API, tipoSolicitudPetroleraId: {}", dto.getTipoSolicitudPetroleraId());
            try {
                PetrolerasClient.TipoSolicitudDTO tipoSol = petrolerasClient.obtenerTipoSolicitud(dto.getTipoSolicitudPetroleraId());
                if (tipoSol != null && tipoSol.getNombre() != null) {
                    solicitud.setSubtipoNombre(tipoSol.getNombre());
                    log.info("subtipoNombre resuelto desde API: '{}'", tipoSol.getNombre());
                }
            } catch (Exception e) {
                log.warn("No se pudo resolver subtipo desde petroleras: {}", e.getMessage());
            }
        }

        solicitud.setEstado(EstadoSolicitud.BORRADOR);
        solicitud.setRutaPdfEditable(rutaPdfEditable);
        if (rutaPdfEditable != null) {
            solicitud.setNombrePdfEditable("editable.pdf");  // Nombre de la plantilla inicial
        }

        SolicitudContrato saved = solicitudRepository.save(solicitud);
        log.info("Solicitud creada exitosamente: {} con tipo: {}", numeroSolicitud, dto.getTipoSolicitud());

        // Notificar al socio de que su trámite ha sido registrado
        try {
            enviarNotificacionSocioEtapa(saved, "NOTIF_SOCIO_CREADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre creación de solicitud: {}", e.getMessage());
        }

        return SolicitudContratoDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public byte[] abrirPdfEditable(Long solicitudId) throws IOException {
        log.info("Obteniendo PDF editable de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getRutaPdfEditable() == null) {
            throw new RuntimeException("La solicitud no tiene PDF editable");
        }

        return pdfService.leerPdf(solicitud.getRutaPdfEditable());
    }

    @Transactional(readOnly = true)
    public byte[] abrirPlantillaOriginal(Long solicitudId) throws IOException {
        log.info("Obteniendo plantilla original de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        return pdfService.leerPlantillaOriginal(solicitud.getNumeroSolicitud());
    }

    @Transactional
    public void guardarPdfEditado(Long solicitudId, MultipartFile pdfEditado) throws IOException {
        log.info("Guardando cambios en PDF editable de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new RuntimeException("Solo se pueden editar solicitudes en estado BORRADOR");
        }

        // Guardar el PDF editado subido por el usuario
        String rutaPdfEditable = pdfService.guardarPdfEditado(
            pdfEditado,
            solicitud.getNumeroSolicitud()
        );

        solicitud.setRutaPdfEditable(rutaPdfEditable);
        solicitud.setNombrePdfEditable(pdfEditado.getOriginalFilename());
        solicitudRepository.save(solicitud);

        log.info("PDF editable actualizado exitosamente con nombre: {}", pdfEditado.getOriginalFilename());
    }

    @Transactional
    public SolicitudContratoDTO enviarASocio(Long solicitudId) throws IOException {
        log.info("Enviando solicitud a socio: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new RuntimeException("Solo se pueden enviar solicitudes en estado BORRADOR");
        }

        // Aplanar PDF (quitar campos editables)
        String rutaPdfEnviado = pdfService.aplanarPdfParaSolicitud(
            solicitud.getRutaPdfEditable(),
            solicitud.getNumeroSolicitud()
        );

        // Actualizar estado y registrar fecha
        solicitud.setRutaPdfEnviado(rutaPdfEnviado);
        // Solo guardar nombre si se subió un PDF editado (no solo plantilla)
        String nombrePdfEnviado = solicitud.getNombrePdfEditable();
        if (nombrePdfEnviado != null && !nombrePdfEnviado.equals("editable.pdf")) {
            // Si se subió un PDF editado, conservar su nombre indicando que fue aplanado
            nombrePdfEnviado = nombrePdfEnviado.replace(".pdf", "_aplanado.pdf");
            solicitud.setNombrePdfEnviado(nombrePdfEnviado);
        }
        // Si solo hay plantilla (editable.pdf), NO guardar nombrePdfEnviado (quedará null)

        solicitud.setEstado(EstadoSolicitud.ENVIADO_SOCIO);
        solicitud.setFechaEnvioSocio(LocalDateTime.now());

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("Solicitud enviada a socio exitosamente. Estado: {}", updated.getEstado());

        // Enviar email al socio notificando el envío del contrato
        try {
            enviarNotificacionSocioEtapa(updated, "NOTIF_SOCIO_ENVIADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre envío: {}", e.getMessage());
        }

        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO subirPdfFirmado(Long solicitudId, MultipartFile pdfFirmado) throws IOException {
        log.info("Subiendo PDF firmado para solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new RuntimeException("Solo se pueden subir PDFs firmados para solicitudes en estado ENVIADO_SOCIO");
        }

        // Guardar PDF firmado
        String rutaPdfFirmado = pdfService.guardarPdfFirmado(
            pdfFirmado,
            solicitud.getNumeroSolicitud()
        );

        // Guardar PDF y fecha, pero NO cambiar estado (se hace manualmente con aceptarFirmaSocio)
        solicitud.setRutaPdfFirmado(rutaPdfFirmado);
        solicitud.setNombrePdfFirmado(pdfFirmado.getOriginalFilename());
        solicitud.setFechaRecepcionFirmado(LocalDateTime.now());

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("PDF firmado subido exitosamente para solicitud: {}", solicitudId);
        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO aceptarFirmaSocio(Long solicitudId) {
        log.info("Aceptando firma del socio para solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new RuntimeException("Solo se puede aceptar la firma en estado ENVIADO_SOCIO");
        }

        if (solicitud.getRutaPdfFirmado() == null) {
            throw new RuntimeException("Debe subir el PDF firmado antes de aceptar la firma");
        }

        solicitud.setEstado(EstadoSolicitud.FIRMADO_SOCIO);
        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("Firma del socio aceptada. Estado: {}", updated.getEstado());
        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO enviarAPetrolera(Long solicitudId) throws IOException {
        log.info("Enviando solicitud a petrolera: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.FIRMADO_SOCIO) {
            throw new RuntimeException("Solo se pueden enviar a petrolera solicitudes en estado FIRMADO_SOCIO");
        }

        // Copiar PDF firmado como final (solo si hay PDF firmado)
        if (solicitud.getRutaPdfFirmado() != null) {
            String rutaPdfFinal = pdfService.copiarPdfFinal(
                solicitud.getRutaPdfFirmado(),
                solicitud.getNumeroSolicitud()
            );
            solicitud.setRutaPdfFinal(rutaPdfFinal);
            solicitud.setNombrePdfFinal("final.pdf");
        }

        // Actualizar estado y registrar fecha
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("Solicitud enviada a petrolera exitosamente. Estado: {}", updated.getEstado());

        // Enviar email a la petrolera (sin crear contrato, se hará en aceptarPorPetrolera)
        try {
            PetrolerasClient.PetroleraDTO petrolera = petrolerasClient.obtenerPetrolera(solicitud.getPetroleraId());
            String emailPetrolera = petrolera.getEmail();
            if (emailPetrolera != null && !emailPetrolera.isEmpty()) {
                SociosClient.SocioDTO socio = sociosClient.obtenerSocio(solicitud.getSocioId());

                java.util.Map<String, String> variables = prepararVariablesContrato(solicitud, socio, petrolera, null);

                String asuntoEmail;
                String cuerpoEmail;

                PetrolerasClient.PlantillaCorreoDTO plantillaCorreo =
                    petrolerasClient.obtenerPlantillaCorreo(solicitud.getPetroleraId(), "CONTRATO_PETROLERA");

                if (plantillaCorreo != null) {
                    asuntoEmail = emailService.procesarPlantilla(plantillaCorreo.getAsunto(), variables);
                    cuerpoEmail = emailService.procesarPlantilla(plantillaCorreo.getCuerpo(), variables);
                } else {
                    String tipoLegible = switch (solicitud.getTipoSolicitud()) {
                        case CAMBIO_CONDICIONES -> "Cambio de Condiciones";
                        case BAJA -> "Baja de Contrato";
                        default -> "Nuevo Contrato";
                    };
                    asuntoEmail = "Solicitud de Contrato - " + solicitud.getNumeroSolicitud() + " - " + socio.getNombre();
                    cuerpoEmail = String.format(
                        "<html><body>" +
                        "<h2>Solicitud de %s</h2>" +
                        "<p>Estimados,</p>" +
                        "<p>Adjuntamos la solicitud de contrato con los siguientes datos:</p>" +
                        "<table style='border-collapse: collapse; margin: 20px 0;'>" +
                        "<tr><td style='padding: 8px; font-weight: bold;'>Nº Solicitud:</td><td style='padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='padding: 8px; font-weight: bold;'>Tipo:</td><td style='padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='padding: 8px; font-weight: bold;'>Socio:</td><td style='padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='padding: 8px; font-weight: bold;'>NIF/CIF:</td><td style='padding: 8px;'>%s</td></tr>" +
                        "</table>" +
                        "<p>Saludos cordiales,<br/>Sistema de Gestión ATG</p>" +
                        "</body></html>",
                        tipoLegible, solicitud.getNumeroSolicitud(), tipoLegible,
                        socio.getNombre(), socio.getNif()
                    );
                }

                emailService.enviarCorreoHTML(emailPetrolera, asuntoEmail, cuerpoEmail);
                registrarEnvioCorreo(updated, "CONTRATO_PETROLERA", emailPetrolera, true, null);
            }
        } catch (Exception e) {
            registrarEnvioCorreo(updated, "CONTRATO_PETROLERA", "petrolera", false, e.getMessage());
            log.error("Error enviando email a petrolera: {}", e.getMessage());
        }

        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO aceptarPorPetrolera(Long solicitudId) {
        log.info("Aceptando solicitud por petrolera: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("Solo se pueden aceptar solicitudes en estado ENVIADO_PETROLERA");
        }

        solicitud.setEstado(EstadoSolicitud.ACEPTADA_PETROLERA);
        solicitud.setFechaResolucionPetrolera(LocalDateTime.now());

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        // Crear o actualizar ContratoSocio según el tipo de solicitud
        try {
            String subtipoNombre = solicitud.getSubtipoNombre();
            log.info("aceptarPorPetrolera - subtipoNombre de solicitud: '{}'", subtipoNombre);

            if (subtipoNombre == null && solicitud.getTipoSolicitudPetroleraId() != null) {
                try {
                    PetrolerasClient.TipoSolicitudDTO tipoSol = petrolerasClient.obtenerTipoSolicitud(solicitud.getTipoSolicitudPetroleraId());
                    if (tipoSol != null) {
                        subtipoNombre = tipoSol.getNombre();
                    }
                } catch (Exception e) {
                    log.warn("aceptarPorPetrolera - no se pudo resolver subtipo: {}", e.getMessage());
                }
            }

            if (solicitud.getTipoSolicitud() == TipoSolicitudContrato.NUEVO) {
                ContratoSocio nuevoContrato = contratoSocioService.crearDesdeSolicitud(solicitud, subtipoNombre);
                updated.setContratoId(nuevoContrato.getId());
                updated = solicitudRepository.save(updated);
                log.info("Contrato activo creado (ID: {}) para solicitud NUEVO", nuevoContrato.getId());
            } else if (solicitud.getTipoSolicitud() == TipoSolicitudContrato.BAJA) {
                if (solicitud.getContratoId() != null) {
                    contratoSocioService.darDeBaja(solicitud.getContratoId(), LocalDate.now());
                    log.info("Contrato (ID: {}) dado de baja", solicitud.getContratoId());
                }
            } else if (solicitud.getTipoSolicitud() == TipoSolicitudContrato.CAMBIO_CONDICIONES) {
                String subtipoHeredado = subtipoNombre;
                if (solicitud.getContratoId() != null) {
                    ContratoSocio contratoAnterior = contratoSocioService.findById(solicitud.getContratoId());
                    if (subtipoHeredado == null && contratoAnterior.getSubtipoContrato() != null) {
                        subtipoHeredado = contratoAnterior.getSubtipoContrato();
                    }
                    contratoSocioService.darDeBaja(solicitud.getContratoId(), LocalDate.now());
                    log.info("Contrato anterior (ID: {}) dado de baja por cambio de condiciones", solicitud.getContratoId());
                }
                ContratoSocio nuevoContrato = contratoSocioService.crearDesdeSolicitud(solicitud, subtipoHeredado);
                updated.setContratoId(nuevoContrato.getId());
                updated = solicitudRepository.save(updated);
                log.info("Nuevo contrato activo creado (ID: {}) para solicitud CAMBIO_CONDICIONES", nuevoContrato.getId());
            }
        } catch (Exception e) {
            log.error("Error al gestionar contrato activo para solicitud {}: {}", solicitudId, e.getMessage());
        }

        // Notificar al socio que la petrolera aceptó
        try {
            enviarNotificacionSocioEtapa(updated, "NOTIF_SOCIO_RESULTADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre aceptación: {}", e.getMessage());
        }

        log.info("Solicitud aceptada por petrolera. Estado: {}", updated.getEstado());
        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO rechazarPorPetrolera(Long solicitudId, String motivoRechazo) {
        log.info("Rechazando solicitud por petrolera: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("Solo se pueden rechazar solicitudes en estado ENVIADO_PETROLERA");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA_PETROLERA);
        solicitud.setFechaResolucionPetrolera(LocalDateTime.now());
        solicitud.setMotivoRechazo(motivoRechazo);

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        // Notificar al socio que la petrolera rechazó
        try {
            enviarNotificacionSocioEtapa(updated, "NOTIF_SOCIO_RESULTADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre rechazo: {}", e.getMessage());
        }

        log.info("Solicitud rechazada por petrolera. Motivo: {}", motivoRechazo);
        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional
    public SolicitudContratoDTO procesarBaja(Long solicitudId) {
        log.info("Procesando solicitud de BAJA: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getTipoSolicitud() != TipoSolicitudContrato.BAJA) {
            throw new RuntimeException("Esta solicitud no es de tipo BAJA");
        }

        if (solicitud.getContratoId() == null) {
            throw new RuntimeException("La solicitud de BAJA debe tener un contrato asociado");
        }

        // Marcar el contrato como inactivo
        ContratoSocio contrato = contratoSocioService.darDeBaja(
            solicitud.getContratoId(),
            LocalDate.now()
        );

        // Obtener información del socio y petrolera
        SociosClient.SocioDTO socio = sociosClient.obtenerSocio(solicitud.getSocioId());
        PetrolerasClient.PetroleraDTO petrolera = petrolerasClient.obtenerPetrolera(solicitud.getPetroleraId());

        // Enviar email a la petrolera
        enviarEmailBajaPetrolera(solicitud, socio, petrolera, contrato);

        // Actualizar estado de la solicitud
        solicitud.setEstado(EstadoSolicitud.ENVIADO_PETROLERA);
        solicitud.setFechaEnvioPetrolera(LocalDateTime.now());

        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("Solicitud de BAJA procesada exitosamente. Contrato {} dado de baja. Email enviado a petrolera.", contrato.getId());

        // Notificar al socio sobre la baja
        try {
            enviarNotificacionSocioEtapa(updated, "NOTIF_SOCIO_RESULTADO");
        } catch (Exception e) {
            log.error("Error notificando socio sobre baja: {}", e.getMessage());
        }

        return SolicitudContratoDTO.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public byte[] descargarPdf(Long solicitudId, TipoPdf tipo) throws IOException {
        log.info("Descargando PDF {} de solicitud: {}", tipo, solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);
        String rutaPdf;

        switch (tipo) {
            case EDITABLE:
                rutaPdf = solicitud.getRutaPdfEditable();
                break;
            case ENVIADO:
                rutaPdf = solicitud.getRutaPdfEnviado();
                break;
            case FIRMADO:
                rutaPdf = solicitud.getRutaPdfFirmado();
                break;
            case FINAL:
                rutaPdf = solicitud.getRutaPdfFinal();
                break;
            default:
                throw new IllegalArgumentException("Tipo de PDF inválido: " + tipo);
        }

        if (rutaPdf == null) {
            throw new RuntimeException("El PDF " + tipo + " no está disponible para esta solicitud");
        }

        return pdfService.leerPdf(rutaPdf);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudContratoDTO> listarSolicitudes(FiltroSolicitudesDTO filtros) {
        log.info("Listando solicitudes con filtros: {}", filtros);

        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filtros.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            filtros.getSortBy()
        );

        Pageable pageable = PageRequest.of(filtros.getPage(), filtros.getSize(), sort);

        Page<SolicitudContrato> page = solicitudRepository.findByFiltros(
            filtros.getSocioId(),
            filtros.getPetroleraId(),
            filtros.getTipoContratoId(),
            filtros.getEstado(),
            filtros.getFechaDesde(),
            filtros.getFechaHasta(),
            pageable
        );

        return page.map(SolicitudContratoDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public SolicitudContratoDTO obtenerPorId(Long id) {
        log.info("Obteniendo solicitud por ID: {}", id);
        return SolicitudContratoDTO.fromEntity(obtenerSolicitudPorId(id));
    }

    @Transactional(readOnly = true)
    public SolicitudContratoDTO obtenerPorNumeroSolicitud(String numeroSolicitud) {
        log.info("Obteniendo solicitud por número: {}", numeroSolicitud);
        SolicitudContrato solicitud = solicitudRepository.findByNumeroSolicitud(numeroSolicitud)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + numeroSolicitud));
        return SolicitudContratoDTO.fromEntity(solicitud);
    }

    @Transactional
    public SolicitudContratoDTO cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        log.info("Cambiando estado de solicitud {} a {}", id, nuevoEstado);

        SolicitudContrato solicitud = obtenerSolicitudPorId(id);
        validarCambioEstado(solicitud.getEstado(), nuevoEstado);

        solicitud.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoSolicitud.ENVIADO_SOCIO) {
            solicitud.setFechaEnvioSocio(LocalDateTime.now());
        } else if (nuevoEstado == EstadoSolicitud.ENVIADO_PETROLERA) {
            solicitud.setFechaEnvioPetrolera(LocalDateTime.now());
        }
        SolicitudContrato updated = solicitudRepository.save(solicitud);

        log.info("Estado cambiado exitosamente");
        return SolicitudContratoDTO.fromEntity(updated);
    }

    private SolicitudContrato obtenerSolicitudPorId(Long id) {
        return solicitudRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));
    }

    private String generarNumeroSolicitud() {
        int year = Year.now().getValue();
        String prefijo = "SOL-" + year + "-";

        Integer maxNumero = solicitudRepository.findMaxNumeroSolicitudByYear(prefijo);
        int siguienteNumero = (maxNumero != null ? maxNumero : 0) + 1;

        return String.format("%s%05d", prefijo, siguienteNumero);
    }

    private void validarCambioEstado(EstadoSolicitud estadoActual, EstadoSolicitud nuevoEstado) {
        // Validar transiciones de estado válidas
        if (estadoActual == EstadoSolicitud.BORRADOR && nuevoEstado != EstadoSolicitud.ENVIADO_SOCIO) {
            throw new RuntimeException("Desde BORRADOR solo se puede pasar a ENVIADO_SOCIO");
        }
        if (estadoActual == EstadoSolicitud.ENVIADO_SOCIO && nuevoEstado != EstadoSolicitud.FIRMADO_SOCIO) {
            throw new RuntimeException("Desde ENVIADO_SOCIO solo se puede pasar a FIRMADO_SOCIO");
        }
        if (estadoActual == EstadoSolicitud.FIRMADO_SOCIO && nuevoEstado != EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("Desde FIRMADO_SOCIO solo se puede pasar a ENVIADO_PETROLERA");
        }
        if (estadoActual == EstadoSolicitud.ENVIADO_PETROLERA) {
            throw new RuntimeException("No se puede cambiar el estado desde ENVIADO_PETROLERA");
        }
    }

    /**
     * Extrae los campos del PDF editable y devuelve información sobre cada campo
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> extraerCamposPdf(Long solicitudId) throws IOException {
        log.info("Extrayendo campos del PDF de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getRutaPdfEditable() == null) {
            throw new RuntimeException("La solicitud no tiene PDF editable");
        }

        return pdfService.extraerCamposFormulario(solicitud.getRutaPdfEditable());
    }

    /**
     * Rellena los campos del PDF con los valores proporcionados y devuelve el PDF como array de bytes
     */
    @Transactional
    public byte[] rellenarCamposPdf(Long solicitudId, java.util.Map<String, String> valores) throws IOException {
        log.info("Rellenando campos del PDF de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getRutaPdfEditable() == null) {
            throw new RuntimeException("La solicitud no tiene PDF editable");
        }

        // Rellenar campos y guardar el PDF
        byte[] pdfRellenado = pdfService.rellenarCamposFormulario(
            solicitud.getRutaPdfEditable(),
            valores
        );

        // Guardar el PDF rellenado en el mismo archivo
        pdfService.guardarPdf(pdfRellenado, solicitud.getRutaPdfEditable());

        // Marcar como editado inline para que el workflow muestre "PDF Editado Guardado"
        solicitud.setNombrePdfEditable("campos_rellenados.pdf");
        solicitudRepository.save(solicitud);

        log.info("PDF rellenado y guardado exitosamente");

        return pdfRellenado;
    }

    /**
     * Obtiene el PDF editable en formato base64 para previsualización
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, String> obtenerPdfBase64(Long solicitudId) throws IOException {
        log.info("Obteniendo PDF en base64 de solicitud: {}", solicitudId);

        SolicitudContrato solicitud = obtenerSolicitudPorId(solicitudId);

        if (solicitud.getRutaPdfEditable() == null) {
            throw new RuntimeException("La solicitud no tiene PDF editable");
        }

        byte[] pdfBytes = pdfService.leerPdf(solicitud.getRutaPdfEditable());
        String base64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);

        java.util.Map<String, String> resultado = new java.util.HashMap<>();
        resultado.put("base64", base64);

        return resultado;
    }

    /**
     * Extrae los campos de una plantilla PDF desde el microservicio de Petroleras
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> extraerCamposPlantilla(Long tipoSolicitudId) throws IOException {
        log.info("Extrayendo campos de la plantilla del tipo de solicitud: {}", tipoSolicitudId);

        // Obtener la plantilla PDF del microservicio de Petroleras
        byte[] plantillaPdf = petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId);

        // Guardar temporalmente el PDF para procesarlo
        String tempFilename = "temp_" + System.currentTimeMillis() + ".pdf";
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), tempFilename);
        Files.write(tempPath, plantillaPdf);

        try {
            // Extraer campos del PDF
            java.util.Map<String, Object> campos = pdfService.extraerCamposFormulario(tempPath.toString());
            log.info("Campos extraídos exitosamente: {} campos",
                ((java.util.List<?>) campos.get("campos")).size());
            return campos;
        } finally {
            // Limpiar archivo temporal
            Files.deleteIfExists(tempPath);
        }
    }

    /**
     * Obtiene la plantilla PDF en formato base64 para previsualización
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, String> obtenerPlantillaBase64(Long tipoSolicitudId) throws IOException {
        log.info("Obteniendo plantilla PDF en base64 del tipo de solicitud: {}", tipoSolicitudId);

        // Obtener la plantilla PDF del microservicio de Petroleras
        byte[] plantillaPdf = petrolerasClient.obtenerPlantillaPdf(tipoSolicitudId);
        String base64 = java.util.Base64.getEncoder().encodeToString(plantillaPdf);

        java.util.Map<String, String> resultado = new java.util.HashMap<>();
        resultado.put("base64", base64);

        return resultado;
    }

    /**
     * Envía email a la petrolera notificando la baja de un contrato
     */
    private void enviarEmailBajaPetrolera(
            SolicitudContrato solicitud,
            SociosClient.SocioDTO socio,
            PetrolerasClient.PetroleraDTO petrolera,
            ContratoSocio contrato) {

        log.info("Enviando email de notificación de BAJA a petrolera: {}", petrolera.getNombre());

        // Preparar variables para la plantilla
        java.util.Map<String, String> variables = prepararVariablesContrato(solicitud, socio, petrolera, contrato);

        // Buscar plantilla unificada CONTRATO_PETROLERA
        PetrolerasClient.PlantillaCorreoDTO plantillaCorreo = petrolerasClient.obtenerPlantillaCorreo(
            solicitud.getPetroleraId(), "CONTRATO_PETROLERA");

        String asunto;
        String cuerpoHTML;

        if (plantillaCorreo != null) {
            asunto = emailService.procesarPlantilla(plantillaCorreo.getAsunto(), variables);
            cuerpoHTML = emailService.procesarPlantilla(plantillaCorreo.getCuerpo(), variables);
        } else {
            asunto = "Notificación de Baja de Contrato - " + socio.getNombre();
            cuerpoHTML = generarCuerpoDefaultBaja(solicitud, socio, contrato);
        }

        // Enviar email
        if (petrolera.getEmail() != null && !petrolera.getEmail().isEmpty()) {
            try {
                emailService.enviarCorreoHTML(petrolera.getEmail(), asunto, cuerpoHTML);
                registrarEnvioCorreo(solicitud, "CONTRATO_PETROLERA", petrolera.getEmail(), true, null);
                log.info("Email de BAJA enviado exitosamente a: {}", petrolera.getEmail());
            } catch (Exception e) {
                registrarEnvioCorreo(solicitud, "CONTRATO_PETROLERA", petrolera.getEmail(), false, e.getMessage());
                log.error("Error enviando email de BAJA a petrolera: {}", e.getMessage());
            }
        } else {
            log.warn("La petrolera {} no tiene email configurado.", petrolera.getNombre());
        }
    }

    private java.util.Map<String, String> prepararVariablesContrato(
            SolicitudContrato solicitud,
            SociosClient.SocioDTO socio,
            PetrolerasClient.PetroleraDTO petrolera,
            ContratoSocio contrato) {

        java.util.Map<String, String> variables = new java.util.HashMap<>();
        variables.put("socio_nombre", socio.getNombre() != null ? socio.getNombre() : "");
        variables.put("socio_nif", socio.getNif() != null ? socio.getNif() : "");
        variables.put("socio_numero", socio.getNumeroSocio() != null ? socio.getNumeroSocio() : "");
        variables.put("petrolera_nombre", petrolera.getNombre() != null ? petrolera.getNombre() : "");
        variables.put("matricula", ""); // Se puede rellenar si hay datos de tarjeta
        variables.put("agrupacion", ""); // Se puede rellenar según datos del contrato
        variables.put("numero_contrato", contrato != null && contrato.getId() != null ? String.valueOf(contrato.getId()) : "");
        variables.put("fecha_actual", LocalDate.now().toString());
        variables.put("observaciones", solicitud.getObservaciones() != null ? solicitud.getObservaciones() : "");
        variables.put("numero_solicitud", solicitud.getNumeroSolicitud() != null ? solicitud.getNumeroSolicitud() : "");
        // Variables unificadas de tipo y subtipo
        String tipoLegible = switch (solicitud.getTipoSolicitud()) {
            case CAMBIO_CONDICIONES -> "Cambio de Condiciones";
            case BAJA -> "Baja";
            default -> "Nuevo Contrato";
        };
        variables.put("tipo_solicitud", tipoLegible);
        variables.put("subtipo", solicitud.getSubtipoNombre() != null ? solicitud.getSubtipoNombre() : "");
        return variables;
    }

    private String generarCuerpoDefaultBaja(SolicitudContrato solicitud, SociosClient.SocioDTO socio, ContratoSocio contrato) {
        StringBuilder cuerpoHTML = new StringBuilder();
        cuerpoHTML.append("<html><body>");
        cuerpoHTML.append("<h2>Notificación de Baja de Contrato</h2>");
        cuerpoHTML.append("<p>Estimados,</p>");
        cuerpoHTML.append("<p>Por medio de la presente, les notificamos la baja del siguiente contrato:</p>");
        cuerpoHTML.append("<table style='border-collapse: collapse; margin: 20px 0;'>");
        cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>Socio:</td><td style='padding: 8px;'>")
            .append(socio.getNombre()).append("</td></tr>");
        cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>NIF/CIF/DNI:</td><td style='padding: 8px;'>")
            .append(socio.getNif()).append("</td></tr>");
        cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>Número de Socio:</td><td style='padding: 8px;'>")
            .append(socio.getNumeroSocio()).append("</td></tr>");
        cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>Fecha de Baja:</td><td style='padding: 8px;'>")
            .append(LocalDate.now().toString()).append("</td></tr>");
        cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>ID Contrato:</td><td style='padding: 8px;'>")
            .append(contrato.getId()).append("</td></tr>");
        if (solicitud.getObservaciones() != null && !solicitud.getObservaciones().isEmpty()) {
            cuerpoHTML.append("<tr><td style='padding: 8px; font-weight: bold;'>Observaciones:</td><td style='padding: 8px;'>")
                .append(solicitud.getObservaciones()).append("</td></tr>");
        }
        cuerpoHTML.append("</table>");
        cuerpoHTML.append("<p>Saludos cordiales,<br/>Sistema de Gestión ATG</p>");
        cuerpoHTML.append("</body></html>");
        return cuerpoHTML.toString();
    }

    private void registrarEnvioCorreo(SolicitudContrato solicitud, String tipoPlantilla, String destinatario, boolean exito, String errorMsg) {
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

    private void enviarNotificacionSocioEtapa(SolicitudContrato solicitud, String tipoNotificacion) {
        SociosClient.SocioDTO socio = sociosClient.obtenerSocio(solicitud.getSocioId());
        PetrolerasClient.PetroleraDTO petrolera = petrolerasClient.obtenerPetrolera(solicitud.getPetroleraId());
        String emailSocio = socio.getEmail();
        if (emailSocio == null || emailSocio.isEmpty()) {
            emailSocio = "socio@example.com";
        }

        String nombreSocio = socio.getNombre();
        String nombrePetrolera = petrolera.getNombre();
        String numSolicitud = solicitud.getNumeroSolicitud();
        String tipoLegible = switch (solicitud.getTipoSolicitud()) {
            case CAMBIO_CONDICIONES -> "Cambio de Condiciones";
            case BAJA -> "Baja";
            default -> "Nuevo Contrato";
        };
        String subtipo = solicitud.getSubtipoNombre() != null ? solicitud.getSubtipoNombre() : "";

        // Mapeo de tipo de notificación a tipo de plantilla en petroleras
        String tipoPlantillaBackend = mapearTipoNotificacionAPlantilla(tipoNotificacion);

        // Intentar obtener plantilla personalizada
        PetrolerasClient.PlantillaCorreoDTO plantillaCorreo = null;
        if (tipoPlantillaBackend != null) {
            plantillaCorreo = petrolerasClient.obtenerPlantillaCorreo(solicitud.getPetroleraId(), tipoPlantillaBackend);
        }

        String asunto;
        String cuerpo;

        if (plantillaCorreo != null) {
            java.util.Map<String, String> variables = new java.util.HashMap<>();
            variables.put("socio_nombre", nombreSocio != null ? nombreSocio : "");
            variables.put("socio_email", emailSocio);
            variables.put("petrolera_nombre", nombrePetrolera != null ? nombrePetrolera : "");
            variables.put("numero_solicitud", numSolicitud != null ? numSolicitud : "");
            variables.put("tipo_solicitud", tipoLegible);
            variables.put("subtipo", subtipo);

            asunto = emailService.procesarPlantilla(plantillaCorreo.getAsunto(), variables);
            cuerpo = emailService.procesarPlantilla(plantillaCorreo.getCuerpo(), variables);
        } else {
            switch (tipoNotificacion) {
                case "NOTIF_SOCIO_CREADO":
                    asunto = "Su solicitud de contrato " + numSolicitud + " ha sido registrada";
                    cuerpo = String.format(
                        "<html><body>" +
                        "<h2>Estimado/a %s</h2>" +
                        "<p>Le informamos que su solicitud de contrato <strong>%s</strong> ha sido registrada correctamente.</p>" +
                        "<p>Tipo de solicitud: <strong>%s</strong></p>" +
                        "<p>Petrolera: <strong>%s</strong></p>" +
                        "<p>Le mantendremos informado/a del estado de su solicitud.</p>" +
                        "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                        "</body></html>",
                        nombreSocio, numSolicitud, tipoLegible, nombrePetrolera);
                    break;
                case "NOTIF_SOCIO_ENVIADO":
                    asunto = "Su contrato " + numSolicitud + " le ha sido enviado para firma";
                    cuerpo = String.format(
                        "<html><body>" +
                        "<h2>Estimado/a %s</h2>" +
                        "<p>Le informamos que el contrato <strong>%s</strong> le ha sido enviado para su revisión y firma.</p>" +
                        "<p>Petrolera: <strong>%s</strong></p>" +
                        "<p>Por favor, revise el documento y devuélvalo firmado a la mayor brevedad posible.</p>" +
                        "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                        "</body></html>",
                        nombreSocio, numSolicitud, nombrePetrolera);
                    break;
                case "NOTIF_SOCIO_RESULTADO":
                    asunto = "Su solicitud de contrato " + numSolicitud + " ha sido enviada a la petrolera";
                    cuerpo = String.format(
                        "<html><body>" +
                        "<h2>Estimado/a %s</h2>" +
                        "<p>Le informamos que su solicitud de contrato <strong>%s</strong> (%s) ha sido enviada a <strong>%s</strong> para su tramitación.</p>" +
                        "<p>Cuando recibamos respuesta, le notificaremos el resultado.</p>" +
                        "<p>Saludos cordiales,<br>Equipo ATG</p>" +
                        "</body></html>",
                        nombreSocio, numSolicitud, tipoLegible, nombrePetrolera);
                    break;
                default:
                    return;
            }
        }

        try {
            emailService.enviarCorreoHTML(emailSocio, asunto, cuerpo);
            registrarEnvioCorreo(solicitud, tipoPlantillaBackend != null ? tipoPlantillaBackend : tipoNotificacion, emailSocio, true, null);
        } catch (Exception e) {
            registrarEnvioCorreo(solicitud, tipoPlantillaBackend != null ? tipoPlantillaBackend : tipoNotificacion, emailSocio, false, e.getMessage());
            log.error("Error en notificación {} al socio: {}", tipoNotificacion, e.getMessage());
        }
    }

    private String mapearTipoNotificacionAPlantilla(String tipoNotificacion) {
        switch (tipoNotificacion) {
            case "NOTIF_SOCIO_CREADO": return "NOTIF_SOCIO_CONTRATO_CREADO";
            case "NOTIF_SOCIO_ENVIADO": return "NOTIF_SOCIO_CONTRATO_ENVIADO";
            case "NOTIF_SOCIO_RESULTADO": return "NOTIF_SOCIO_CONTRATO_RESULTADO";
            default: return null;
        }
    }

    public enum TipoPdf {
        EDITABLE, ENVIADO, FIRMADO, FINAL
    }
}
