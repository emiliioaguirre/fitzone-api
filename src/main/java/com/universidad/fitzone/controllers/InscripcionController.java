package com.universidad.fitzone.controllers;

import com.universidad.fitzone.models.Inscripcion;
import com.universidad.fitzone.services.InscripcionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inscripciones")
public class InscripcionController {
    private final InscripcionService servicio;

    public InscripcionController(InscripcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Inscripcion> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Inscripcion consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Inscripcion inscribirEnClase(@RequestBody Inscripcion datos) {
        return servicio.inscribirEnClase(datos);
    }
}
