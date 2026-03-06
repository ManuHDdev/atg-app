package com.manuhd.app.tarjetas.repository;

import com.manuhd.app.tarjetas.model.PlantillaTarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantillaTarjetaRepository extends JpaRepository<PlantillaTarjeta, Long> {

    Optional<PlantillaTarjeta> findByTipo(TipoPlantilla tipo);

    Optional<PlantillaTarjeta> findByTipoAndActivaTrue(TipoPlantilla tipo);
}
