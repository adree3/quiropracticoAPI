package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {
    /**
     * Busca las citas de un clinete en especifico ordenado primero las mas recientes
     * @param clienteId identificador del cliente
     * @return lista de citas
     */
    List<Cita> findByClienteIdClienteOrderByFechaHoraInicioDesc(Integer clienteId);

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
     * @param idExcluir cita a excluir de la busqueda
     * @return lista de citas de ese rango
     */
    @Query("SELECT c FROM Cita c WHERE c.quiropractico.idUsuario = :quiroId " +
            "AND c.estado != 'cancelada' " +
            "AND c.fechaHoraInicio < :nuevoFin " +
            "AND c.fechaHoraFin > :nuevoInicio " +
            "AND (:idExcluir IS NULL OR c.idCita != :idExcluir)")
    List<Cita> findCitasConflictivas(
            @Param("quiroId") Integer quiroId,
            @Param("nuevoInicio") LocalDateTime nuevoInicio,
            @Param("nuevoFin") LocalDateTime nuevoFin,
            @Param("idExcluir") Integer idExcluir
    );

    /**
     * Busca cualquier cita activa que olape con el rango indicado
     * @param inicio inicio del rango
     * @param fin fin del rango
     * @return lista de citas
     */
    @Query("SELECT c FROM Cita c WHERE " +
            "c.estado <> com.example.quiropracticoapi.model.enums.EstadoCita.cancelada AND " +
            "c.fechaHoraInicio < :fin AND " +
            "c.fechaHoraFin > :inicio")
    List<Cita> findCitasConflictivasGlobales(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    /**
     * Cuenta las citas en un rango
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @return citas sumadas
     */
    long countByFechaHoraInicioBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Cuenta las citas en un rango y en un estado definido
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @param estado estado de la cita
     * @return citas sumadas
     */
    long countByFechaHoraInicioBetweenAndEstado(LocalDateTime inicio, LocalDateTime fin, EstadoCita estado);

    /**
     * Busca citas futuras que tengan cobrado con el bono del propietario
     * @param pacienteId identificador del paciente
     * @param propietarioId identificardor del propietario del bono
     * @return lista de citas cobradas con el bono del propietario
     */
    @Query("SELECT c FROM Cita c " +
            "WHERE c.cliente.idCliente = :pacienteId " +
            "AND c.consumoBono.bonoActivo.cliente.idCliente = :propietarioId " +
            "AND c.fechaHoraInicio > CURRENT_TIMESTAMP " +
            "AND c.estado != 'cancelada'")
    List<Cita> findCitasFuturasConBonoPrestado(
            @Param("pacienteId") Integer pacienteId,
            @Param("propietarioId") Integer propietarioId);
}
