package com.manuhd.app.dispositivos.repository;

import com.manuhd.app.dispositivos.model.EstadoSolicitud;
import com.manuhd.app.dispositivos.model.SolicitudDispositivo;
import com.manuhd.app.dispositivos.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Mayor secuencial usado en los numeros de solicitud del anio ("DIS-2026-"). El prefijo
     * ocupa 9 caracteres, asi que el secuencial empieza en la posicion 10.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(s.numeroSolicitud, 10) AS int)) FROM SolicitudDispositivo s "
            + "WHERE s.numeroSolicitud LIKE :prefijo%")
    Integer findMaxNumeroSolicitudByYear(@Param("prefijo") String prefijo);
}
