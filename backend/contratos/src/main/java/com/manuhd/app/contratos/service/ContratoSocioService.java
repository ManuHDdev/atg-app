package com.manuhd.app.contratos.service;

import com.manuhd.app.contratos.model.*;
import com.manuhd.app.contratos.repository.ContratoSocioRepository;
import com.manuhd.app.contratos.repository.PlantillaContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratoSocioService {

    private final ContratoSocioRepository contratoRepository;
    private final PlantillaContratoRepository plantillaRepository;
    private final MapeoPlantillaCamposService mapeoService;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public List<ContratoSocio> findAll() {
        log.info("Buscando todos los contratos");
        return contratoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ContratoSocio findById(Long id) {
        log.info("Buscando contrato con id: {}", id);
        return contratoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ContratoSocio> findBySocioId(Long socioId) {
        log.info("Buscando contratos del socio: {}", socioId);
        return contratoRepository.findBySocioId(socioId);
    }

    @Transactional(readOnly = true)
    public List<ContratoSocio> findByEstado(EstadoContrato estado) {
        log.info("Buscando contratos con estado: {}", estado);
        return contratoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<ContratoSocio> findByPetroleraId(Long petroleraId) {
        log.info("Buscando contratos de la petrolera: {}", petroleraId);
        return contratoRepository.findByPetroleraId(petroleraId);
    }

    @Transactional(readOnly = true)
    public List<ContratoSocio> findActivosBySocioAndPetrolera(Long socioId, Long petroleraId) {
        log.info("Buscando contratos activos del socio {} con petrolera {}", socioId, petroleraId);
        return contratoRepository.findBySocioIdAndPetroleraIdAndActivoTrue(socioId, petroleraId);
    }

    @Transactional(readOnly = true)
    public List<Long> findPetrolerasConContratosActivosBySocio(Long socioId) {
        log.info("Buscando petroleras con contratos activos para el socio: {}", socioId);
        List<ContratoSocio> contratosActivos = contratoRepository.findBySocioIdAndActivoTrue(socioId);
        return contratosActivos.stream()
                .map(ContratoSocio::getPetroleraId)
                .distinct()
                .toList();
    }

    @Transactional
    public ContratoSocio create(ContratoSocio contrato) {
        log.info("Creando nuevo contrato para socio: {}", contrato.getSocioId());

        // Validaciones
        if (contrato.getSocioId() == null) {
            throw new RuntimeException("Debe especificar un socio para el contrato");
        }

        if (contrato.getTarjetaId() == null) {
            throw new RuntimeException("Debe especificar una tarjeta para el contrato");
        }

        contrato.setEstado(EstadoContrato.BORRADOR);

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio crearDesdeSolicitud(SolicitudContrato solicitud, String subtipoContrato) {
        log.info("Creando contrato activo desde solicitud: {} (tipo: {})",
                solicitud.getNumeroSolicitud(), solicitud.getTipoSolicitud());

        ContratoSocio contrato = new ContratoSocio();
        contrato.setSocioId(solicitud.getSocioId());
        contrato.setEmpresaId(solicitud.getEmpresaId());
        contrato.setTarjetaId(solicitud.getTarjetaId());
        contrato.setPetroleraId(solicitud.getPetroleraId());
        contrato.setTipoContrato(solicitud.getTipoSolicitud().name());
        contrato.setSubtipoContrato(subtipoContrato);
        contrato.setTipoSolicitante(solicitud.getEsAutonomo() ? "AUTONOMO" : "EMPRESA");
        contrato.setFechaHoraSolicitud(solicitud.getFechaHoraSolicitud());
        contrato.setSolicitadoPor(solicitud.getSolicitadoPor());
        contrato.setSolicitudId(solicitud.getId());
        contrato.setEstado(EstadoContrato.COMPLETADO);
        contrato.setActivo(true);
        contrato.setFechaVigenciaDesde(LocalDate.now());
        contrato.setObservaciones(solicitud.getObservaciones());

        ContratoSocio saved = contratoRepository.save(contrato);
        log.info("Contrato activo creado con ID: {} para socio: {}", saved.getId(), saved.getSocioId());
        return saved;
    }

    @Transactional
    public ContratoSocio generarBorrador(Long contratoId, Map<String, String> datosAdicionales) throws IOException {
        log.info("Generando borrador del contrato: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        if (contrato.getPlantilla() == null) {
            throw new RuntimeException("El contrato debe tener una plantilla asociada");
        }

        PlantillaContrato plantilla = contrato.getPlantilla();

        if (plantilla.getRutaArchivo() == null || plantilla.getRutaArchivo().isEmpty()) {
            throw new RuntimeException("La plantilla no tiene archivo PDF asociado");
        }

        // Obtener mapeos de campos
        List<MapeoPlantillaCampos> mapeos = mapeoService.findByPlantillaIdActivos(plantilla.getId());

        // Preparar datos para rellenar el PDF
        Map<String, String> datosPdf = new HashMap<>();

        for (MapeoPlantillaCampos mapeo : mapeos) {
            String valor = obtenerValorCampo(contrato, mapeo.getCampoEntidad());
            if (valor != null) {
                datosPdf.put(mapeo.getNombreCampoPdf(), valor);
            }
        }

        // Agregar datos adicionales
        if (datosAdicionales != null) {
            datosPdf.putAll(datosAdicionales);
        }

        // Generar PDF con los datos
        String rutaBorrador = pdfService.rellenarPdfConDatos(plantilla.getRutaArchivo(), datosPdf);

        contrato.setRutaBorrador(rutaBorrador);
        contrato.setEstado(EstadoContrato.BORRADOR);

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio enviarASocio(Long contratoId) {
        log.info("Enviando contrato a socio: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        if (contrato.getRutaBorrador() == null || contrato.getRutaBorrador().isEmpty()) {
            throw new RuntimeException("Debe generar el borrador antes de enviar el contrato");
        }

        contrato.setEstado(EstadoContrato.ENVIADO_SOCIO);
        contrato.setFechaEnvioSocio(LocalDateTime.now());

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio recibirFirmado(Long contratoId, String rutaFirmado) {
        log.info("Recibiendo contrato firmado: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        contrato.setRutaFirmado(rutaFirmado);
        contrato.setEstado(EstadoContrato.FIRMADO_SOCIO);
        contrato.setFechaRecepcionFirmado(LocalDateTime.now());

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio enviarAPetrolera(Long contratoId) {
        log.info("Enviando contrato a petrolera: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        if (contrato.getRutaFirmado() == null || contrato.getRutaFirmado().isEmpty()) {
            throw new RuntimeException("Debe recibir el contrato firmado antes de enviarlo a la petrolera");
        }

        contrato.setEstado(EstadoContrato.ENVIADO_PETROLERA);
        contrato.setFechaEnvioPetrolera(LocalDateTime.now());

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio completar(Long contratoId, String rutaFinal) {
        log.info("Completando contrato: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        contrato.setRutaFinal(rutaFinal);
        contrato.setEstado(EstadoContrato.COMPLETADO);

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio cancelar(Long contratoId, String observaciones) {
        log.info("Cancelando contrato: {}", contratoId);

        ContratoSocio contrato = findById(contratoId);

        contrato.setEstado(EstadoContrato.CANCELADO);
        contrato.setObservaciones(observaciones);

        return contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio update(Long id, ContratoSocio contratoActualizado) {
        log.info("Actualizando contrato con id: {}", id);

        ContratoSocio contrato = findById(id);

        contrato.setTipoContrato(contratoActualizado.getTipoContrato());
        contrato.setObservaciones(contratoActualizado.getObservaciones());

        return contratoRepository.save(contrato);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando contrato con id: {}", id);
        ContratoSocio contrato = findById(id);
        contrato.setActivo(false);
        contrato.setDeletedAt(LocalDateTime.now());
        contratoRepository.save(contrato);
    }

    @Transactional
    public ContratoSocio darDeBaja(Long contratoId, LocalDate fechaBaja) {
        log.info("Dando de baja contrato: {} con fecha: {}", contratoId, fechaBaja);

        ContratoSocio contrato = findById(contratoId);

        if (!contrato.getActivo()) {
            throw new RuntimeException("El contrato ya está dado de baja");
        }

        contrato.setActivo(false);
        contrato.setFechaVigenciaHasta(fechaBaja != null ? fechaBaja : java.time.LocalDate.now());

        ContratoSocio updated = contratoRepository.save(contrato);
        log.info("Contrato dado de baja exitosamente. Vigencia hasta: {}", updated.getFechaVigenciaHasta());

        return updated;
    }

    private String obtenerValorCampo(ContratoSocio contrato, String nombreCampo) {
        if (nombreCampo == null) return null;

        return switch (nombreCampo) {
            case "socioId" -> contrato.getSocioId() != null ? contrato.getSocioId().toString() : null;
            case "empresaId" -> contrato.getEmpresaId() != null ? contrato.getEmpresaId().toString() : null;
            case "tarjetaId" -> contrato.getTarjetaId() != null ? contrato.getTarjetaId().toString() : null;
            case "petroleraId" -> contrato.getPetroleraId() != null ? contrato.getPetroleraId().toString() : null;
            case "tipoContrato" -> contrato.getTipoContrato();
            default -> null;
        };
    }
}
