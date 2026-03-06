package com.manuhd.app.petroleras.repository;

import com.manuhd.app.petroleras.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoSolicitudRepository extends JpaRepository<TipoSolicitud, Long> {

    List<TipoSolicitud> findByPetroleraId(Long petroleraId);

    List<TipoSolicitud> findByPetroleraIdAndActiva(Long petroleraId, Boolean activa);

    Optional<TipoSolicitud> findByCodigo(String codigo);

    List<TipoSolicitud> findByActivaOrderByOrdenAsc(Boolean activa);

    List<TipoSolicitud> findByPetroleraIdOrderByOrdenAsc(Long petroleraId);
}
