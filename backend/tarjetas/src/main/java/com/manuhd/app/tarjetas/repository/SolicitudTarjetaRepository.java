package com.manuhd.app.tarjetas.repository;

import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Mayor secuencial usado en los números de solicitud del año ("TAR-2026-"). El prefijo
     * ocupa 9 caracteres, así que el secuencial empieza en la posición 10.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(s.numeroSolicitud, 10) AS int)) FROM SolicitudTarjeta s WHERE s.numeroSolicitud LIKE :prefijo%")
    Integer findMaxNumeroSolicitudByYear(@Param("prefijo") String prefijo);
}
