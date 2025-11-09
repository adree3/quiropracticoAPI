package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {
    /**
     * Busca las citas de un clinete es especifico
     * @param clienteId identificador del cliente
     * @return lista de citas
     */
    List<Cita> findByClienteId(Integer clienteId);

    /**
     * Busca las citas de un quiropráctico
     * @param quiropracticoId identificador del quiropráctico
     * @return lista de citas
     */
    List<Cita> findByQuiropracticoId( Integer quiropracticoId);

    /**
     * Busca las citas de un quiropractico entre el rango de dos fechas
     * @param quiropracticoId identificador quiropráctico
     * @param fechaInicio primera fecha
     * @param fechaFin segunda fecha
     * @return lista de citas
     */
    List<Cita> findByQuiropracticoIdAndFechaHoraInicioBetween(Integer quiropracticoId, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Busca todas las citas (de cualquier quiropractico) entre un rango de fechas
     * @param fechaInicio primera fecha
     * @param fechaFin segunda fecha
     * @return
     */
    List<Cita> findByFechaHoraInicioBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
