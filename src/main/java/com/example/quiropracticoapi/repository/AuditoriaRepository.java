package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByEntidad(String entidad, Pageable pageable);

    Page<Auditoria> findByUsernameResponsable(String username, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE " +
            "(:entidad IS NULL OR a.entidad = :entidad) AND " +
            "(:fechaInicio IS NULL OR a.fechaHora >= :fechaInicio) AND " +
            "(:fechaFin IS NULL OR a.fechaHora <= :fechaFin) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "   LOWER(a.detalles) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(a.usernameResponsable) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(a.accion) LIKE LOWER(CONCAT('%', :search, '%')) " +
            ")")
    Page<Auditoria> buscarConFiltros(
            @Param("entidad") String entidad,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("search") String search,
            Pageable pageable
    );
}