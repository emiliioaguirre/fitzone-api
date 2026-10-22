package com.universidad.fitzone.services;

import com.universidad.fitzone.models.Socio;
import com.universidad.fitzone.repositories.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class SocioService {
    private final SocioRepository repositorio;

    public SocioService(SocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Socio> listar() {
        return repositorio.findAll();
    }

    public Socio consultar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Socio no encontrado"));
    }

    public Socio registrarSocio(Socio socio) {
        if (socio.getId() != null || socio.getNombre() == null || socio.getNombre().isBlank()
                || socio.getCorreo() == null || !socio.getCorreo().contains("@")
                || socio.getVigenciaMembresia() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren nombre, correo y vigencia, sin ID");
        }
        return repositorio.save(socio);
    }

    public boolean consultarMembresia(Long id) {
        Socio socio = consultar(id);
        return !socio.getVigenciaMembresia().isBefore(LocalDate.now());
    }
}
