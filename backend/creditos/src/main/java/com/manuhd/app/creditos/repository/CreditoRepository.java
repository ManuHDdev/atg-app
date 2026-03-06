package com.manuhd.app.creditos.repository;

import com.manuhd.app.creditos.model.Credito;
import com.manuhd.app.creditos.model.EstadoCredito;
import com.manuhd.app.creditos.model.TipoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditoRepository extends JpaRepository<Credito, Long> {

    List<Credito> findBySocioId(Long socioId);

    List<Credito> findByPetroleraId(Long petroleraId);

    List<Credito> findByEstado(EstadoCredito estado);

    List<Credito> findByTipoCredito(TipoCredito tipoCredito);

    List<Credito> findByProgramadoEnvioTrueAndFechaProgramadaEnvio(LocalDate fecha);

    @Query("SELECT c FROM Credito c WHERE c.socioId = :socioId AND c.estado = :estado")
    List<Credito> findBySocioIdAndEstado(@Param("socioId") Long socioId, @Param("estado") EstadoCredito estado);

    @Query("SELECT c FROM Credito c WHERE c.petroleraId = :petroleraId AND c.estado = :estado")
    List<Credito> findByPetroleraIdAndEstado(@Param("petroleraId") Long petroleraId, @Param("estado") EstadoCredito estado);
}
