package com.manuhd.app.dispositivos.repository;

import com.manuhd.app.dispositivos.model.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    List<Dispositivo> findBySocioId(Long socioId);

    List<Dispositivo> findBySocioIdAndActivoTrue(Long socioId);

    Optional<Dispositivo> findByMatriculaAndActivoTrue(String matricula);

    List<Dispositivo> findByPetroleraId(Long petroleraId);

    List<Dispositivo> findByActivoTrue();
}
