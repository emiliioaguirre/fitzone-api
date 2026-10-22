package com.universidad.fitzone.controllers;

import com.universidad.fitzone.models.Sucursal;
import com.universidad.fitzone.services.SucursalService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sucursales")
public class SucursalController {
    private final SucursalService servicio;

    public SucursalController(SucursalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Sucursal> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Sucursal consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Sucursal registrarSucursal(@RequestBody Sucursal datos) {
        return servicio.registrarSucursal(datos);
    }
}
