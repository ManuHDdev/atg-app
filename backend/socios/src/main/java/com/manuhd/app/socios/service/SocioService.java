package com.manuhd.app.socios.service;

import com.manuhd.app.socios.model.Socio;
import com.manuhd.app.socios.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SocioService {
    private final SocioRepository socioRepository;
    
    public List<Socio> findAll() {
        return socioRepository.findAll();
    }
    
    public Socio findById(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado: " + id));
    }
    
    public Socio create(Socio socio) {
        // Establecer fechaAlta automáticamente si no está presente
        if (socio.getFechaAlta() == null) {
            socio.setFechaAlta(LocalDateTime.now());
        }
        return socioRepository.save(socio);
    }
    
    public Socio update(Long id, Socio socioUpdate) {
        Socio socio = findById(id);

        // Actualizar todos los campos editables
        socio.setNombre(socioUpdate.getNombre());
        socio.setDireccion(socioUpdate.getDireccion());
        socio.setPoblacion(socioUpdate.getPoblacion());
        socio.setProvincia(socioUpdate.getProvincia());
        socio.setCodigoPostal(socioUpdate.getCodigoPostal());
        socio.setEmail(socioUpdate.getEmail());
        socio.setTelefono(socioUpdate.getTelefono());
        socio.setAgrupacion(socioUpdate.getAgrupacion());
        socio.setNumeroSocio(socioUpdate.getNumeroSocio());
        socio.setEsAutonomo(socioUpdate.getEsAutonomo());
        socio.setActivo(socioUpdate.getActivo());

        // fechaAlta NO se actualiza - es inmutable después de la creación

        return socioRepository.save(socio);
    }
    
    public void delete(Long id) {
        socioRepository.deleteById(id);
    }
}
