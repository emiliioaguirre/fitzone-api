package com.universidad.fitzone.controllers;

import com.universidad.fitzone.models.Socio;
import com.universidad.fitzone.services.SocioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/socios")
public class SocioController {
    private final SocioService servicio;

    public SocioController(SocioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Socio> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Socio consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Socio registrarSocio(@RequestBody Socio datos) {
        return servicio.registrarSocio(datos);
    }

    @GetMapping("/{id}/membresia")
    public boolean consultarMembresia(@PathVariable Long id) {
        return servicio.consultarMembresia(id);
    }
}
