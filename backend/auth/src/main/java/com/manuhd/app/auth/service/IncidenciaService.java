package com.manuhd.app.auth.service;

import com.manuhd.app.auth.dto.*;
import com.manuhd.app.auth.model.*;
import com.manuhd.app.auth.repository.ComentarioRepository;
import com.manuhd.app.auth.repository.IncidenciaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final ComentarioRepository comentarioRepository;
    private final MailNotificationService mailService;

    public IncidenciaService(IncidenciaRepository incidenciaRepository,
                             ComentarioRepository comentarioRepository,
                             MailNotificationService mailService) {
        this.incidenciaRepository = incidenciaRepository;
        this.comentarioRepository = comentarioRepository;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public List<IncidenciaResumenDto> listar() {
        return incidenciaRepository.findAllByOrderByFechaCreacionDesc()
                .stream().map(IncidenciaResumenDto::from).toList();
    }

    @Transactional(readOnly = true)
    public IncidenciaDto obtener(Long id) {
        return IncidenciaDto.from(findOrThrow(id));
    }

    public IncidenciaDto crear(CreateIncidenciaRequest req) {
        Incidencia i = new Incidencia();
        i.setTitulo(req.titulo());
        i.setDescripcion(req.descripcion());
        i.setTipo(req.tipo());
        i.setPrioridad(req.prioridad());
        i.setEstado(EstadoIncidencia.NUEVA);
        i.setAutor(usuarioActual());
        Incidencia saved = incidenciaRepository.save(i);
        mailService.notificarNuevaIncidencia(saved);
        return IncidenciaDto.from(saved);
    }

    public IncidenciaDto cambiarEstado(Long id, CambiarEstadoRequest req) {
        requireDeveloper();
        Incidencia i = findOrThrow(id);
        i.setEstado(req.nuevoEstado());
        if (req.notasDeveloper() != null) {
            i.setNotasDeveloper(req.notasDeveloper());
        }
        return IncidenciaDto.from(incidenciaRepository.save(i));
    }

    public ComentarioDto addComentario(Long incidenciaId, AddComentarioRequest req) {
        Incidencia i = findOrThrow(incidenciaId);
        Comentario c = new Comentario();
        c.setIncidencia(i);
        c.setTexto(req.texto());
        c.setAutor(usuarioActual());
        return ComentarioDto.from(comentarioRepository.save(c));
    }

    public void eliminarComentario(Long incidenciaId, Long comentarioId) {
        requireDeveloper();
        Comentario c = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new NoSuchElementException("Comentario no encontrado"));
        if (!c.getIncidencia().getId().equals(incidenciaId)) {
            throw new IllegalArgumentException("El comentario no pertenece a esta incidencia");
        }
        comentarioRepository.delete(c);
    }

    public void eliminar(Long id) {
        requireDeveloper();
        incidenciaRepository.delete(findOrThrow(id));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Incidencia findOrThrow(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incidencia #" + id + " no encontrada"));
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "desconocido";
    }

    private void requireDeveloper() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isDev = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DEVELOPER"::equals);
        if (!isDev) throw new AccessDeniedException("Solo el DEVELOPER puede realizar esta acción");
    }
}
