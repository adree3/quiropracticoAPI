package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.ConsumoBono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumoBonoRepository extends JpaRepository<ConsumoBono, Integer> {
    /**
     * Busca todos los usos asociados a un bono
     * @param bonoActivoId identificador del bono
     * @return lista de usos del bono
     */
    List<ConsumoBono> findByBonoActivoIdBonoActivo(Integer bonoActivoId);

    /**
     * Busca todos los usos asociados a un bono ordenados por fecha de consumo (historial).
     * @param bonoActivoId identificador del bono
     * @return lista de usos del bono
     */
    List<ConsumoBono> findByBonoActivoIdBonoActivoOrderByFechaConsumoAsc(Integer bonoActivoId);
    /**
     * Busca el uso asociado a
     * una cita (para verificar si una cita especifica ya desconto un uso del bono)
     * @param citaId indetificador del bono
     * @return el uso del bono si existe
     */
    Optional<ConsumoBono> findByCitaIdCita(Integer citaId);

    /**
     * Elimina directamente el consumo vinculado a una cita mediante JPQL,
     * evitando conflictos con el CascadeType.ALL de la relación Cita -> ConsumoBono.
     */
    @Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ConsumoBono c WHERE c.cita.idCita = :idCita")
    void deleteByCitaIdCita(@org.springframework.data.repository.query.Param("idCita") Integer idCita);
}
