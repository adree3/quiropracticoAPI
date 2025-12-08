package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Busca los pagos por rango de fechas
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @return lista de pagos
     */
    List<Pago> findByFechaPagoBetweenOrderByFechaPagoDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Busca los pagos pendientes
     * @return lista de pagos pendientes
     */
    List<Pago> findByPagadoFalseOrderByFechaPagoDesc();

    /**
     * Suma los ingresos de un rango
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @return la suma de los ingresos
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    BigDecimal sumIngresosBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
