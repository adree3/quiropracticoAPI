package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Cita> findByClienteIdCliente(Integer clienteId);

    /**
     * Busca las citas de un quiropráctico
     * @param quiropracticoId identificador del quiropráctico
     * @return lista de citas
     */
    List<Cita> findByQuiropracticoIdUsuario( Integer quiropracticoId);

    /**
     * Busca las citas de un quiropractico entre el rango de dos fechas
     * @param quiropracticoId identificador quiropráctico
     * @param fechaInicio primera fecha
     * @param fechaFin segunda fecha
     * @return lista de citas
     */
    List<Cita> findByQuiropracticoIdUsuarioAndFechaHoraInicioBetween(Integer quiropracticoId, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Busca todas las citas (de cualquier quiropractico) entre un rango de fechas
     * @param fechaInicio primera fecha
     * @param fechaFin segunda fecha
     * @return lista de citas
     */
    List<Cita> findByFechaHoraInicioBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Busca cita que se solapen con el rango propuesto. Ignorando las cancelada
     * @param quiroId identificador quiropractico.
     * @param nuevoInicio fecha inicio comprobar
     * @param nuevoFin fecha fin comprobar
     * @return lista de citas de ese rango
     */
    @Query("SELECT c FROM Cita c WHERE c.quiropractico.idUsuario = :quiroId " +
            "AND c.estado != 'cancelada' " +
            "AND c.fechaHoraInicio < :nuevoFin " +
            "AND c.fechaHoraFin > :nuevoInicio")
    List<Cita> findCitasConflictivas(
            @Param("quiroId") Integer quiroId,
            @Param("nuevoInicio") LocalDateTime nuevoInicio,
            @Param("nuevoFin") LocalDateTime nuevoFin
    );
}
