package com.universidad.fitzone.controllers;

import com.universidad.fitzone.models.Clase;
import com.universidad.fitzone.services.ClaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clases")
public class ClaseController {
    private final ClaseService servicio;

    public ClaseController(ClaseService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Clase> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Clase consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Clase registrarClase(@RequestBody Clase datos) {
        return servicio.registrarClase(datos);
    }

    @GetMapping("/{id}/cupo")
    public int consultarCupo(@PathVariable Long id) {
        return servicio.consultarCupo(id);
    }
}
