package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.ConsumoBono;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<ConsumoBono> findByBonoActivoId(Integer bonoActivoId);

    /**
     * Busca el uso asociado a
     * una cita (para verificar si una cita especifica ya desconto un uso del bono)
     * @param citaId indetificador del bono
     * @return el uso del bono si existe
     */
    Optional<ConsumoBono> findByCitaId(Integer citaId);
}
