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

    /**
     * Créditos en un estado dado que NO llevan una fecha de envío programada propia.
     * Los créditos con programación explícita quedan reservados al job diario
     * {@code procesarCreditosProgramados}: si un gestor fija el envío para el día 20,
     * el envío por día de semana no debe adelantarlo al 15.
     */
    @Query("SELECT c FROM Credito c WHERE c.estado = :estado " +
            "AND (c.programadoEnvio IS NULL OR c.programadoEnvio = false OR c.fechaProgramadaEnvio IS NULL)")
    List<Credito> findByEstadoSinProgramacionPropia(@Param("estado") EstadoCredito estado);

    @Query("SELECT c FROM Credito c WHERE c.socioId = :socioId AND c.estado = :estado")
    List<Credito> findBySocioIdAndEstado(@Param("socioId") Long socioId, @Param("estado") EstadoCredito estado);

    @Query("SELECT c FROM Credito c WHERE c.petroleraId = :petroleraId AND c.estado = :estado")
    List<Credito> findByPetroleraIdAndEstado(@Param("petroleraId") Long petroleraId, @Param("estado") EstadoCredito estado);
}
