package com.manuhd.app.petroleras.repository;

import com.manuhd.app.petroleras.model.SubseccionPetrolera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubseccionPetroleraRepository extends JpaRepository<SubseccionPetrolera, Long> {
    List<SubseccionPetrolera> findByPetroleraIdAndActivaTrueOrderByOrdenAsc(Long petroleraId);
    List<SubseccionPetrolera> findByPetroleraIdOrderByOrdenAsc(Long petroleraId);
}
