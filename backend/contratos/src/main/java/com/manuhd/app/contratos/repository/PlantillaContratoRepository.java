package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.PlantillaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaContratoRepository extends JpaRepository<PlantillaContrato, Long> {

    List<PlantillaContrato> findByActivaTrue();

    List<PlantillaContrato> findByPetroleraId(Long petroleraId);

    List<PlantillaContrato> findByPetroleraIdAndActivaTrue(Long petroleraId);

    List<PlantillaContrato> findByPetroleraIdAndTipoContratoId(Long petroleraId, Long tipoContratoId);

    List<PlantillaContrato> findByPetroleraIdAndTipoContratoIdAndActivaTrue(Long petroleraId, Long tipoContratoId);

    Optional<PlantillaContrato> findByPetroleraIdAndTipoContratoIdAndTipoSolicitudPetroleraIdAndActivaTrue(
        Long petroleraId, Long tipoContratoId, Long tipoSolicitudPetroleraId);

    @Query("SELECT p FROM PlantillaContrato p WHERE p.petroleraId = :petroleraId " +
           "AND p.tipoContratoId = :tipoContratoId " +
           "AND p.tipoSolicitudPetroleraId IS NULL " +
           "AND p.activa = true")
    Optional<PlantillaContrato> findPlantillaGenerica(
        @Param("petroleraId") Long petroleraId,
        @Param("tipoContratoId") Long tipoContratoId);
}
