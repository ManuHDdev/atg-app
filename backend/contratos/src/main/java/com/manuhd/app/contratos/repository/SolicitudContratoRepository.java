package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.EstadoSolicitud;
import com.manuhd.app.contratos.model.SolicitudContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudContratoRepository extends JpaRepository<SolicitudContrato, Long> {

    Optional<SolicitudContrato> findByNumeroSolicitud(String numeroSolicitud);

    List<SolicitudContrato> findBySocioId(Long socioId);

    List<SolicitudContrato> findByPetroleraId(Long petroleraId);

    List<SolicitudContrato> findByEstado(EstadoSolicitud estado);

    Page<SolicitudContrato> findByEstado(EstadoSolicitud estado, Pageable pageable);

    @Query("SELECT s FROM SolicitudContrato s WHERE " +
           "(:socioId IS NULL OR s.socioId = :socioId) AND " +
           "(:petroleraId IS NULL OR s.petroleraId = :petroleraId) AND " +
           "(:tipoContratoId IS NULL OR s.tipoContratoId = :tipoContratoId) AND " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR s.fechaHoraSolicitud >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR s.fechaHoraSolicitud <= :fechaHasta)")
    Page<SolicitudContrato> findByFiltros(
        @Param("socioId") Long socioId,
        @Param("petroleraId") Long petroleraId,
        @Param("tipoContratoId") Long tipoContratoId,
        @Param("estado") EstadoSolicitud estado,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );

    @Query("SELECT MAX(CAST(SUBSTRING(s.numeroSolicitud, 10) AS int)) FROM SolicitudContrato s WHERE s.numeroSolicitud LIKE :prefijo%")
    Integer findMaxNumeroSolicitudByYear(@Param("prefijo") String prefijo);
}
