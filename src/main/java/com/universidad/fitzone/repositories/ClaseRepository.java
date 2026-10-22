package com.universidad.fitzone.repositories;

import com.universidad.fitzone.models.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    // revisar y ocupar el cupo en la misma transacción
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> buscarConBloqueo(@Param("id") Long id);
}
