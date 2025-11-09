package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.BonoActivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonoActivoRepository extends JpaRepository<BonoActivo, Integer> {
    /**
     * Busca todos los bonos del cliente indicado
     * @param clienteId identificador del cliente
     * @return lista de bonos, gastados o no
     */
    List<BonoActivo> findByClienteIdCliente(Integer clienteId);
    /**
     * Busca los bonos de un cliente según las sesiones que tenga
     * @param clienteId indetificador del cliente
     * @param sesiones numero de sesiones (normalmente 0)
     * @return lista de bonos con x sesiones
     */
    List<BonoActivo> findByClienteIdClienteAndSesionesRestantesGreaterThan(Integer clienteId, int sesiones);}
