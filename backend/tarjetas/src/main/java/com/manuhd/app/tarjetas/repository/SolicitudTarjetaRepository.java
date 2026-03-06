package com.manuhd.app.tarjetas.repository;

import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudTarjetaRepository extends JpaRepository<SolicitudTarjeta, Long> {

    List<SolicitudTarjeta> findBySocioId(Long socioId);

    List<SolicitudTarjeta> findByPetroleraId(Long petroleraId);

    List<SolicitudTarjeta> findByEstado(EstadoSolicitud estado);

    List<SolicitudTarjeta> findByTipo(TipoSolicitud tipo);

    List<SolicitudTarjeta> findByTipoAndEstado(TipoSolicitud tipo, EstadoSolicitud estado);

    List<SolicitudTarjeta> findByMatricula(String matricula);

    List<SolicitudTarjeta> findBySocioIdAndEstado(Long socioId, EstadoSolicitud estado);
}
