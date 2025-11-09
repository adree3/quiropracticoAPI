package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    /**
     * Busca todos los bloques de horario (la disponibilidad general) del
     * quiropractico indicado
     * @param quiropracticoId identificador del quiropráctico
     * @return lista de horarios
     */
    List<Horario> findByQuiropracticoId(Integer quiropracticoId);

    /**
     * Busca los bloques de horario de un quiropractico para un día específico
     * @param quiropracticoId identificador del quiropráctico
     * @param diaSemana del 1 al 7 (lunes a domingo)
     * @return lista de horarios de un quiropráctico en un día
     */
    List<Horario> findByQuiropracticoIdAndDiaSemana(Integer quiropracticoId, Byte diaSemana);

}
