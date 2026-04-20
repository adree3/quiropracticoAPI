package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
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
     * Devuelve los documentos inactivos (en la papelera) de un cliente, ordenados por fecha descendente de borrado.
     */
    List<DocumentoCliente> findByClienteIdClienteAndActivoFalseOrderByFechaEliminacionLogicaDesc(Integer idCliente);
    
    /**
     * Devuelve los documentos activos vinculados a una cita específica.
     */
    List<DocumentoCliente> findByCitaIdCitaAndActivoTrueOrderByFechaSubidaDesc(Integer idCita);

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

    /**
     * Comprueba si ya existe un documento activo de tipo firma (JUSTIFICANTE_ASISTENCIA)
     * vinculado a una cita concreta. Usado para garantizar la unicidad de firma por cita.
     */
    @Query("SELECT COUNT(d) > 0 FROM DocumentoCliente d WHERE d.cita.idCita = :idCita " +
           "AND d.tipoDocumento = :tipo AND d.estadoSubida = 'ACTIVO' AND d.activo = true")
    boolean existsFirmaActivaParaCita(@Param("idCita") Integer idCita,
                                      @Param("tipo") TipoDocumento tipo);

    /**
     * Busca un documento por su ruta exacta en R2.
     */
    java.util.Optional<DocumentoCliente> findByPathArchivo(String pathArchivo);
}
