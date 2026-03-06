package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.TipoSolicitudPetrolera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoSolicitudPetroleraRepository extends JpaRepository<TipoSolicitudPetrolera, Long> {

    List<TipoSolicitudPetrolera> findByPetroleraId(Long petroleraId);

    List<TipoSolicitudPetrolera> findByPetroleraIdAndTipoContratoId(Long petroleraId, Long tipoContratoId);

    List<TipoSolicitudPetrolera> findByPetroleraIdAndTipoContratoIdAndActivoTrue(Long petroleraId, Long tipoContratoId);

    List<TipoSolicitudPetrolera> findByPetroleraIdOrderByOrdenAsc(Long petroleraId);

    List<TipoSolicitudPetrolera> findByActivoTrue();
}
