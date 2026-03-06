package com.manuhd.app.tarjetas.repository;

import com.manuhd.app.tarjetas.model.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {
    
    List<Tarjeta> findBySocioId(Long socioId);
    
    List<Tarjeta> findByPetroleraId(Long petroleraId);
    
    List<Tarjeta> findByActivaTrue();
    
    List<Tarjeta> findBySocioIdAndActivaTrue(Long socioId);

    List<Tarjeta> findByMatricula(String matricula);
}
