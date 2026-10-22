package com.universidad.fitzone.repositories;

import com.universidad.fitzone.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsBySocioIdAndClaseId(Long socio, Long clase);
    long countByClaseId(Long clase);
    List<Inscripcion> findByRecordatorioEnviadoFalse();
}
