package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Busca todos los pagos indicados realizados en un rango de fechas
     * @param inicio fecha y hora de inicio
     * @param fin fecha y hora de fin
     * @return pageable de pagos
     */
    Page<Pago> findByFechaPagoBetweenAndPagadoOrderByFechaPagoDesc(LocalDateTime inicio, LocalDateTime fin, boolean pagado, Pageable pageable);

    /**
     * Busca los pagos pendientes
     * @return lista de pagos pendientes
     */
    Page<Pago> findByPagadoFalseOrderByFechaPagoDesc(Pageable pageable);


    /**
     * Obtiene un page de pagos ya pagados en un rango de fechas y opcionalmente
     * con un texto de filtrado por nombre completo o servicio
     * @param inicio rango inicio
     * @param fin rango fin
     * @param search filtrado opcional
     * @param pageable -
     * @return un page de los pagos pagados filtrados
     */
    @Query("SELECT p FROM Pago p WHERE p.pagado = true " +
            "AND p.fechaPago BETWEEN :inicio AND :fin " +
            "AND (:search IS NULL OR :search = '' OR (" +
            "   LOWER(p.cliente.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(p.cliente.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(p.servicioPagado.nombreServicio) LIKE LOWER(CONCAT('%', :search, '%'))" +
            "))")
    Page<Pago> findHistorialWithSearch(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Obtiene un page de pagos pendientes con el buscador opcional
     * @param search buscador opcional
     * @param pageable -
     * @return un page de pagos pendientes filtrados
     */
    @Query("SELECT p FROM Pago p WHERE p.pagado = false " +
            "AND (:search IS NULL OR :search = '' OR (" +
            "   LOWER(p.cliente.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(p.cliente.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(p.servicioPagado.nombreServicio) LIKE LOWER(CONCAT('%', :search, '%'))" +
            "))")
    Page<Pago> findPendientesWithSearch(
            @Param("search") String search,
            Pageable pageable);

    /**
     * Suma los ingresos de un rango
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @return la suma de los ingresos
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.pagado = true AND p.fechaPago BETWEEN :inicio AND :fin")
    Double sumTotalCobradoEnRango(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Suma la deuda global
     * @return la suma de los pagos pendientes
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.pagado = false")
    Double sumTotalPendienteGlobal();
}
