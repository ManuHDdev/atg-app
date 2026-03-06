package com.manuhd.app.auth.repository;

import com.manuhd.app.auth.model.EstadoIncidencia;
import com.manuhd.app.auth.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findAllByOrderByFechaCreacionDesc();
    List<Incidencia> findByEstadoOrderByFechaCreacionDesc(EstadoIncidencia estado);
}
