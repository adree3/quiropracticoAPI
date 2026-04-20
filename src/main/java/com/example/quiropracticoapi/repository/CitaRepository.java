package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * @param pageable paginacion
     * @return pagina de citas
     */
    Page<Cita> findByClienteIdClienteOrderByFechaHoraInicioDesc(Integer clienteId, Pageable pageable);

    /**
     * Busca las citas de un clinete en especifico filtrando por estado
     * @param clienteId identificador del cliente
     * @param estado estado de la cita
     * @param pageable paginacion
     * @return pagina de citas
     */
    Page<Cita> findByClienteIdClienteAndEstadoOrderByFechaHoraInicioDesc(Integer clienteId, EstadoCita estado, Pageable pageable);

    @Query("SELECT c FROM Cita c WHERE " +
            "c.cliente.idCliente = :idCliente AND " +
            "(:estado IS NULL OR c.estado = :estado) AND " +
            "(:fechaInicio IS NULL OR c.fechaHoraInicio >= :fechaInicio) AND " +
            "(:fechaFin IS NULL OR c.fechaHoraInicio <= :fechaFin)")
    Page<Cita> findByClienteAndFiltros(
            @Param("idCliente") Integer idCliente,
            @Param("estado") EstadoCita estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

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

    /**
     * Busca todas las citas filtrando por nombre/apellidos de usuario, teléfono, estado y rango de fechas.
     */
    @Query("SELECT c FROM Cita c WHERE " +
            "(:search IS NULL OR " +
            "LOWER(CONCAT(c.cliente.nombre, ' ', c.cliente.apellidos)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.cliente.telefono LIKE CONCAT('%', :search, '%') OR " +
            "CAST(c.idCita AS string) LIKE CONCAT('%', :search, '%')) AND " +
            "(:estado IS NULL OR c.estado = :estado) AND " +
            "(CAST(:fechaInicio AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio >= :fechaInicio) AND " +
            "(CAST(:fechaFin AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio <= :fechaFin)")
    Page<Cita> findAllWithFilters(
            @Param("search") String search,
            @Param("estado") EstadoCita estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM Cita c WHERE " +
            "(:search IS NULL OR " +
            "LOWER(CONCAT(c.cliente.nombre, ' ', c.cliente.apellidos)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.cliente.telefono LIKE CONCAT('%', :search, '%') OR " +
            "CAST(c.idCita AS string) LIKE CONCAT('%', :search, '%')) AND " +
            "(:estado IS NULL OR c.estado = :estado) AND " +
            "(CAST(:fechaInicio AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio >= :fechaInicio) AND " +
            "(CAST(:fechaFin AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio <= :fechaFin)")
    long countAllCitasInFilters(
            @Param("search") String search,
            @Param("estado") EstadoCita estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT COUNT(c) FROM Cita c WHERE " +
            "(:search IS NULL OR " +
            "LOWER(CONCAT(c.cliente.nombre, ' ', c.cliente.apellidos)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.cliente.telefono LIKE CONCAT('%', :search, '%') OR " +
            "CAST(c.idCita AS string) LIKE CONCAT('%', :search, '%')) AND " +
            "(:estado IS NULL OR c.estado = :estado) AND " +
            "c.estado = :kpiEstado AND " +
            "(CAST(:fechaInicio AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio >= :fechaInicio) AND " +
            "(CAST(:fechaFin AS java.time.LocalDateTime) IS NULL OR c.fechaHoraInicio <= :fechaFin)")
    long countCitasByEstadoInFilters(
            @Param("search") String search,
            @Param("estado") EstadoCita estado,
            @Param("kpiEstado") EstadoCita kpiEstado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}
