package com.universidad.fitzone.services;

import com.universidad.fitzone.models.Clase;
import com.universidad.fitzone.models.Socio;
import com.universidad.fitzone.models.Inscripcion;
import com.universidad.fitzone.models.InscripcionRegistrada;
import com.universidad.fitzone.repositories.ClaseRepository;
import com.universidad.fitzone.repositories.InscripcionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class InscripcionService {
    private final InscripcionRepository repositorio;
    private final ClaseRepository clases;
    private final SocioService socios;
    private final ApplicationEventPublisher eventos;

    public InscripcionService(InscripcionRepository repositorio, ClaseRepository clases,
            SocioService socios, ApplicationEventPublisher eventos) {
        this.repositorio = repositorio;
        this.clases = clases;
        this.socios = socios;
        this.eventos = eventos;
    }

    public List<Inscripcion> listar() {
        return repositorio.findAll();
    }

    public Inscripcion consultar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inscripción no encontrada"));
    }

    @Transactional
    public Inscripcion inscribirEnClase(Inscripcion inscripcion) {
        if (inscripcion.getId() != null || inscripcion.getSocio() == null
                || inscripcion.getSocio().getId() == null || inscripcion.getClase() == null
                || inscripcion.getClase().getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren socio y clase, sin ID");
        }
        Clase clase = clases.buscarConBloqueo(inscripcion.getClase().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clase no encontrada"));
        Socio socio = socios.consultar(inscripcion.getSocio().getId());
        if (!socios.consultarMembresia(socio.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "MEMBRESIA_VENCIDA: la membresía no está vigente");
        }
        if (!clase.getInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(CONFLICT, "CLASE_INICIADA: ya no se permiten inscripciones");
        }
        if (repositorio.existsBySocioIdAndClaseId(socio.getId(), clase.getId())) {
            throw new ResponseStatusException(CONFLICT, "INSCRIPCION_DUPLICADA: el socio ya está inscrito");
        }
        if (repositorio.countByClaseId(clase.getId()) >= clase.getCupo()) {
            throw new ResponseStatusException(CONFLICT, "SIN_CUPO: la clase está completa");
        }

        // guardar la inscripción con sus datos registrados
        inscripcion.setSocio(socio);
        inscripcion.setClase(clase);
        inscripcion.setFecha(LocalDateTime.now());
        inscripcion.setConfirmacionEnviada(false);
        inscripcion.setRecordatorioEnviado(false);
        Inscripcion guardada = repositorio.save(inscripcion);
        eventos.publishEvent(new InscripcionRegistrada(guardada.getId()));
        return guardada;
    }
}
