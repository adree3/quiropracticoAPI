package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByEntidadOrderByFechaHoraDesc(String entidad);

    List<Auditoria> findByUsernameResponsableOrderByFechaHoraDesc(String username);
}