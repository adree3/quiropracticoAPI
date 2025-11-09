package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    /**
     * Busca todos los pagos realizados de un cliente
     * @param clienteId identificador del cliente
     * @return lista de pagos
     */
    List<Pago> findByClienteIdCliente(Integer clienteId);

    /**
     * Busca todos los pagos realizzados en un rango de fechas
     * @param fechaInicio fecha y hora de inicio
     * @param fechaFin fecha y hora de fin
     * @return lista de pagos
     */
    List<Pago> findByFechaPagoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

}
