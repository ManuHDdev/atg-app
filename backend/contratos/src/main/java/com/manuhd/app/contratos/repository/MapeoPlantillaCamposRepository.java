package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.MapeoPlantillaCampos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapeoPlantillaCamposRepository extends JpaRepository<MapeoPlantillaCampos, Long> {
    
    List<MapeoPlantillaCampos> findByPlantillaId(Long plantillaId);
    
    List<MapeoPlantillaCampos> findByPlantillaIdAndActivoTrue(Long plantillaId);
}
