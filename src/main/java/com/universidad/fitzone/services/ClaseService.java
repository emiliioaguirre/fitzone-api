package com.universidad.fitzone.services;

import com.universidad.fitzone.models.Clase;
import com.universidad.fitzone.repositories.ClaseRepository;
import com.universidad.fitzone.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class ClaseService {
    private final ClaseRepository repositorio;
    private final SucursalService sucursales;
    private final InscripcionRepository inscripciones;

    public ClaseService(ClaseRepository repositorio, SucursalService sucursales,
            InscripcionRepository inscripciones) {
        this.repositorio = repositorio;
        this.sucursales = sucursales;
        this.inscripciones = inscripciones;
    }

    public List<Clase> listar() {
        return repositorio.findAll();
    }

    public Clase consultar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clase no encontrada"));
    }

    public Clase registrarClase(Clase clase) {
        if (clase.getId() != null || clase.getSucursal() == null || clase.getSucursal().getId() == null
                || clase.getTipo() == null || clase.getInicio() == null
                || clase.getCupo() == null || clase.getCupo() < 1 || clase.getCupo() > 20) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren sucursal, tipo, inicio y cupo de 1 a 20, sin ID");
        }
        if (!clase.getInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "La clase debe iniciar en una fecha futura");
        }
        clase.setSucursal(sucursales.consultar(clase.getSucursal().getId()));
        return repositorio.save(clase);
    }

    public int consultarCupo(Long id) {
        Clase clase = consultar(id);
        return clase.getCupo() - (int) inscripciones.countByClaseId(id);
    }
}
