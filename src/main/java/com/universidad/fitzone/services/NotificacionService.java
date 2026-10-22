package com.universidad.fitzone.services;

import com.universidad.fitzone.models.Inscripcion;
import com.universidad.fitzone.repositories.InscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private final InscripcionRepository repositorio;

    public NotificacionService(InscripcionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void confirmarInscripcion(Long id) {
        Inscripcion inscripcion = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inscripción no encontrada"));
        if (!inscripcion.isConfirmacionEnviada()) {
            log.info("[confirmación] inscripción {} de {} en {} para {}", id,
                    inscripcion.getSocio().getNombre(), inscripcion.getClase().getTipo(),
                    inscripcion.getSocio().getCorreo());
            inscripcion.setConfirmacionEnviada(true);
        }
        // si faltan menos de dos horas, avisar al confirmar
        enviarRecordatorio(inscripcion, LocalDateTime.now());
        repositorio.save(inscripcion);
    }

    @Transactional
    public int enviarRecordatorios(LocalDateTime ahora) {
        int enviados = 0;
        for (Inscripcion inscripcion : repositorio.findByRecordatorioEnviadoFalse()) {
            if (inscripcion.isConfirmacionEnviada() && enviarRecordatorio(inscripcion, ahora)) {
                repositorio.save(inscripcion);
                enviados++;
            }
        }
        return enviados;
    }

    private boolean enviarRecordatorio(Inscripcion inscripcion, LocalDateTime ahora) {
        LocalDateTime inicio = inscripcion.getClase().getInicio();
        if (inscripcion.isRecordatorioEnviado() || !inicio.isAfter(ahora)
                || inicio.minusHours(2).isAfter(ahora)) {
            return false;
        }
        log.info("[recordatorio] {}: tu clase de {} inicia a las {}. Correo: {}",
                inscripcion.getSocio().getNombre(), inscripcion.getClase().getTipo(), inicio,
                inscripcion.getSocio().getCorreo());
        inscripcion.setRecordatorioEnviado(true);
        return true;
    }
}
