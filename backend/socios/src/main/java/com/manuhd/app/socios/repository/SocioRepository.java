package com.manuhd.app.socios.repository;

import com.manuhd.app.socios.model.Agrupacion;
import com.manuhd.app.socios.model.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocioRepository extends JpaRepository<Socio, Long> {
    
    Optional<Socio> findByNumeroSocio(String numeroSocio);
    
    List<Socio> findByActivoTrue();
    
    List<Socio> findByAgrupacion(Agrupacion agrupacion);
    
    @Query("SELECT s FROM Socio s WHERE s.activo = true AND s.agrupacion = :agrupacion")
    List<Socio> findByActivoTrueAndAgrupacion(Agrupacion agrupacion);
    
    @Query("SELECT s FROM Socio s WHERE LOWER(s.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.numeroSocio) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Socio> searchSocios(String search);
}
