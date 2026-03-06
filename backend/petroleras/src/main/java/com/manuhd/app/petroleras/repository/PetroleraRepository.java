package com.manuhd.app.petroleras.repository;

import com.manuhd.app.petroleras.model.Petrolera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetroleraRepository extends JpaRepository<Petrolera, Long> {
    
    Optional<Petrolera> findByNombre(String nombre);
    
    List<Petrolera> findByActivaTrue();
}
