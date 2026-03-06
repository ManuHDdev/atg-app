package com.manuhd.app.contratos.repository;

import com.manuhd.app.contratos.model.TipoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoContratoRepository extends JpaRepository<TipoContrato, Long> {

    Optional<TipoContrato> findByCodigo(String codigo);

    List<TipoContrato> findByActivoTrue();

    boolean existsByCodigo(String codigo);
}
