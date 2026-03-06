package com.manuhd.app.auth.repository;

import com.manuhd.app.auth.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByIncidenciaIdOrderByFechaAsc(Long incidenciaId);
}
