package com.universidad.fitzone.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class RecordatorioScheduler {
    private final NotificacionService notificaciones;

    public RecordatorioScheduler(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    // revisar avisos pendientes cada cinco segundos
    @Scheduled(fixedDelayString = "${fitzone.recordatorios.delay:5000}",
            initialDelayString = "${fitzone.recordatorios.delay:5000}")
    public void revisarRecordatorios() {
        notificaciones.enviarRecordatorios(LocalDateTime.now());
    }
}
