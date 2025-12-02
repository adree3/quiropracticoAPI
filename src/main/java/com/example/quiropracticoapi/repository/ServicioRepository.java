package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
    /**
     * Busca todos los servicios activos
     * @return lista de servicios
     */
    List<Servicio> findByActivoTrue();

    /**
     * Busca todos los servicios activos de un tipo especifico (ej: todos los bonos activos)
     * @param tipo saber que tipo de servicio tiene
     * @return lista de servicios
     */
    List<Servicio> findByActivoTrueAndTipo(TipoServicio tipo);

    /**
     * Busca los servicios activos
     * @param activo indica si el servicio esta activo o no
     * @return lista de servicios
     */
    List<Servicio> findByActivo(Boolean activo);
}
