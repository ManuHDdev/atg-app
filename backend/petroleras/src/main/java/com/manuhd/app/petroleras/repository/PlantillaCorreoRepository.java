package com.manuhd.app.petroleras.repository;

import com.manuhd.app.petroleras.model.PlantillaCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaCorreoRepository extends JpaRepository<PlantillaCorreo, Long> {

    List<PlantillaCorreo> findByPetroleraId(Long petroleraId);

    Optional<PlantillaCorreo> findByPetroleraIdAndTipoPlantilla(Long petroleraId, String tipoPlantilla);

    List<PlantillaCorreo> findByPetroleraIdAndActiva(Long petroleraId, Boolean activa);
}
