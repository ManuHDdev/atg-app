package com.manuhd.app.socios.repository;

import com.manuhd.app.socios.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    
    List<Empresa> findBySocioId(Long socioId);
    
    List<Empresa> findByActivaTrue();
    
    List<Empresa> findBySocioIdAndActivaTrue(Long socioId);
}
