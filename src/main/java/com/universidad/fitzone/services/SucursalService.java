package com.universidad.fitzone.services;

import com.universidad.fitzone.models.Sucursal;
import com.universidad.fitzone.repositories.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class SucursalService {
    private final SucursalRepository repositorio;

    public SucursalService(SucursalRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Sucursal> listar() {
        return repositorio.findAll();
    }

    public Sucursal consultar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Sucursal no encontrada"));
    }

    public Sucursal registrarSucursal(Sucursal sucursal) {
        if (sucursal.getId() != null || sucursal.getNombre() == null || sucursal.getNombre().isBlank()
                || sucursal.getDireccion() == null || sucursal.getDireccion().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren nombre y dirección, sin ID");
        }
        return repositorio.save(sucursal);
    }
}
