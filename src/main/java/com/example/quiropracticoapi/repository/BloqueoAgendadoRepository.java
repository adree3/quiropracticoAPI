package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoAgendadoRepository extends JpaRepository<BloqueoAgenda, Integer> {
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

}
