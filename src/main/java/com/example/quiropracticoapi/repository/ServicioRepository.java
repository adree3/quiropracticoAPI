package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
    /**
     * Busca todos los servicios activos
     * @return lista de servicios
     */
    Page<Servicio> findByActivoTrue(Pageable pageable);

    /**
     * Obtiene una lista de todos los servicios activos ordenados alfabeticamente
     * @return lista de servicios
     */
    List<Servicio> findByActivoTrueOrderByNombreServicioAsc();

    /**
     * Busca todos los servicios activos de un tipo especifico (ej: todos los bonos activos)
     * @param tipo saber que tipo de servicio tiene
     * @return lista de servicios
     */
    List<Servicio> findByActivoTrueAndTipo(TipoServicio tipo);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Servicio s WHERE :activo IS NULL OR s.activo = :activo")
    Page<Servicio> findServiciosPaginados(@org.springframework.data.repository.query.Param("activo") Boolean activo, Pageable pageable);
}
