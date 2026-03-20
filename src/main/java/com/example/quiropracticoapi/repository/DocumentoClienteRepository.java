package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentoClienteRepository extends JpaRepository<DocumentoCliente, Integer> {

    /**
     * Devuelve los documentos activos de un cliente, ordenados por fecha descendente.
     */
    List<DocumentoCliente> findByClienteIdClienteAndActivoTrueOrderByFechaSubidaDesc(Integer idCliente);

    /**
     * Busca registros atascados en PENDIENTE más antiguos que un tiempo dado.
     * Usado por el Cronjob de limpieza para detectar subidas fallidas.
     */
    @Query("SELECT d FROM DocumentoCliente d WHERE d.estadoSubida = 'PENDIENTE' AND d.fechaSubida < :umbralFecha")
    List<DocumentoCliente> findRegistrosPendientesAntiguos(@Param("umbralFecha") LocalDateTime umbralFecha);

    /**
     * Devuelve todos los documentos en estado ERROR_SUBIDA para revisión del admin.
     */
    List<DocumentoCliente> findByEstadoSubida(EstadoSubida estadoSubida);
}
