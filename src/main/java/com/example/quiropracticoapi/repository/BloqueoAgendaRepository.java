package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Integer> {
    /**
     * Busca los bloqueos que tenga un quiropráctico entre dos rangos
     * @param quiroId indetificador del quiropráctico
     * @param rangoInicio incio del rango (ej. hoy a las 00:00)
     * @param rangoFin fin del rango (ej. hoy a las 23:59)
     * @return lista de bloqueos del quiropractico en ese rango
     */
    @Query("SELECT b FROM BloqueoAgenda b WHERE " +
            "b.usuario.idUsuario = :quiroId AND " +
            "b.fechaHoraInicio < :rangoFin AND b.fechaHoraFin > :rangoInicio")
    List<BloqueoAgenda> findBloqueosPersonalesQueSolapan(
            @Param("quiroId") Integer quiroId,
            @Param("rangoInicio") LocalDateTime rangoInicio,
            @Param("rangoFin") LocalDateTime rangoFin
    );

    /**
     * Busca los bloqueos de la clinica en general entre dos rangos
     * @param rangoInicio inicio del rango
     * @param rangoFin fin del rango
     * @return lista de bloqueos de la clinica (normalmente festivos) en el rango
     */
    @Query("SELECT b FROM BloqueoAgenda b WHERE " +
            "b.usuario.idUsuario IS NULL AND " +
            "b.fechaHoraInicio < :rangoFin AND b.fechaHoraFin > :rangoInicio")
    List<BloqueoAgenda> findBloqueosClinicaQueSolapan(
            @Param("rangoInicio") LocalDateTime rangoInicio,
            @Param("rangoFin") LocalDateTime rangoFin
    );

    /**
     * Coge los bloqueosAgenda futuros
     * @param fecha para indicar el rango
     * @return lista de bloqueo de agenda
     */
    List<BloqueoAgenda> findByFechaHoraInicioAfterOrderByFechaHoraInicio(LocalDateTime fecha);

    /**
     * Busca los conflictos que puede haber al editar un bloqueo personal (excluyendo el propio)
     * @param usuarioId identificador del empleado
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @param bloqueoIdIgnorar identificador del bloqueo para editar
     * @return lista de bloqueos que tengan conflicto
     */
    @Query("SELECT b FROM BloqueoAgenda b WHERE " +
            "((b.fechaHoraInicio < :fin) AND (b.fechaHoraFin > :inicio)) " +
            "AND b.usuario.idUsuario = :usuarioId " +
            "AND b.idBloqueo != :bloqueoIdIgnorar")
    List<BloqueoAgenda> findConflictoUsuarioExcluyendoId(
            @Param("usuarioId") Integer usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("bloqueoIdIgnorar") Integer bloqueoIdIgnorar
    );

    /**
     * Busca los conflictos que puede haber al editar un bloqueo global (excluyendo el propio)
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @param bloqueoIdIgnorar identificador del bloqueo para editar
     * @return lista de bloqueos que tengan conflicto
     */
    @Query("SELECT b FROM BloqueoAgenda b WHERE " +
            "((b.fechaHoraInicio < :fin) AND (b.fechaHoraFin > :inicio)) " +
            "AND b.usuario IS NULL " +
            "AND b.idBloqueo != :bloqueoIdIgnorar")
    List<BloqueoAgenda> findConflictoGlobalExcluyendoId(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("bloqueoIdIgnorar") Integer bloqueoIdIgnorar
    );

    /**
     * Busca los bloqueos que afecten a un dia especifico
     * @param inicioDia inicio de dia
     * @param finDia fin de dia
     * @return lista de bloqueos
     */
    @Query("SELECT b FROM BloqueoAgenda b WHERE " +
            "(b.fechaHoraInicio <= :finDia AND b.fechaHoraFin >= :inicioDia) " +
            "AND (b.usuario IS NULL)")
    List<BloqueoAgenda> findBloqueosGlobalesPorFecha(
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia
    );
}
