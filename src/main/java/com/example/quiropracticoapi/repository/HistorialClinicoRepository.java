package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.HistorialClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, Integer> {
    /**
     * Busca el historial de una cita indicada
     * @param citaId indetificador de una cita
     * @return historial de la cita
     */
    Optional<HistorialClinico> findByCitaIdCita(Integer citaId);

    /**
     * Busca todo el historial clinico de un cliente de
     * forma descendente(el mas reciente primero)
     * @param clienteId identificador del clinete
     * @return lista de historiales del cliente
     */
    List<HistorialClinico> findByClienteIdClienteOrderByFechaSesionDesc(Integer clienteId);


}
