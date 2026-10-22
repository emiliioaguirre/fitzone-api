package com.universidad.fitzone.services;

import com.universidad.fitzone.models.InscripcionRegistrada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InscripcionListener {
    private static final Logger log = LoggerFactory.getLogger(InscripcionListener.class);
    private final NotificacionService notificaciones;

    public InscripcionListener(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    // confirmar después de guardar la inscripción
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarInscripcion(InscripcionRegistrada evento) {
        try {
            notificaciones.confirmarInscripcion(evento.getIdInscripcion());
        } catch (RuntimeException error) {
            log.error("[confirmación] inscripción {}: {}", evento.getIdInscripcion(), error.getMessage());
        }
    }
}
