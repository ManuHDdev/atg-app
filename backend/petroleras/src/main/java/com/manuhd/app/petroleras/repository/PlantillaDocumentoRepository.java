package com.manuhd.app.petroleras.repository;

import com.manuhd.app.petroleras.enums.ModuloDocumento;
import com.manuhd.app.petroleras.model.PlantillaDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaDocumentoRepository extends JpaRepository<PlantillaDocumento, Long> {

    List<PlantillaDocumento> findByActivoTrueOrderByPetroleraIdAscModuloAscTipoSolicitudAsc();

    List<PlantillaDocumento> findByPetroleraIdAndActivoTrueOrderByModuloAscTipoSolicitudAsc(Long petroleraId);

    /** Plantilla viva (no borrada) para una clave. Se use o no, ocupa la clave. */
    Optional<PlantillaDocumento> findByPetroleraIdAndModuloAndTipoSolicitudAndActivoTrue(
            Long petroleraId, ModuloDocumento modulo, String tipoSolicitud);

    /** Plantilla utilizable por los consumidores: viva y habilitada. */
    Optional<PlantillaDocumento> findByPetroleraIdAndModuloAndTipoSolicitudAndActivaTrueAndActivoTrue(
            Long petroleraId, ModuloDocumento modulo, String tipoSolicitud);
}
