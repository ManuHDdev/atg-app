package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.ContratoSocio;
import com.manuhd.app.contratos.model.EstadoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoSocioRepository extends JpaRepository<ContratoSocio, Long> {
    
    List<ContratoSocio> findBySocioId(Long socioId);
    
    List<ContratoSocio> findByEstado(EstadoContrato estado);
    
    List<ContratoSocio> findByPetroleraId(Long petroleraId);

    List<ContratoSocio> findBySocioIdAndEstado(Long socioId, EstadoContrato estado);

    List<ContratoSocio> findBySocioIdAndPetroleraIdAndActivoTrue(Long socioId, Long petroleraId);

    List<ContratoSocio> findBySocioIdAndActivoTrue(Long socioId);
}
