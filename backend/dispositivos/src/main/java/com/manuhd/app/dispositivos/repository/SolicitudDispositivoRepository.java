package com.manuhd.app.dispositivos.repository;

import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SolicitudDispositivoRepository extends JpaRepository<SolicitudDispositivo, Long> {

    List<SolicitudDispositivo> findBySocioId(Long socioId);

    List<SolicitudDispositivo> findByPetroleraId(Long petroleraId);

    List<SolicitudDispositivo> findByEstado(EstadoSolicitud estado);

    List<SolicitudDispositivo> findByTipoSolicitud(TipoSolicitud tipoSolicitud);

    List<SolicitudDispositivo> findByProgramadoEnvioTrueAndFechaProgramadaEnvio(LocalDate fecha);
}
