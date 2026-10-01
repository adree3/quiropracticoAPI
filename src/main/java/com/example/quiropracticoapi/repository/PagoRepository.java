package com.example.quiropracticoapi.repository;
import com.example.quiropracticoapi.model.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @EntityGraph(attributePaths = {"cliente", "servicioPagado"})
    List<Pago> findByClienteIdCliente(Integer clienteId);

    /**
     * Búsqueda paginada y filtrada con parámetros dinámicos procesada nativamente en MySQL.
     * Incorpora EntityGraph para eliminar cualquier problema de N+1 sobre cliente y servicio.
     */
    @EntityGraph(attributePaths = {"cliente", "servicioPagado"})
    @Query("SELECT p FROM Pago p " +
           "LEFT JOIN p.servicioPagado s " +
           "WHERE (:pagado IS NULL OR p.pagado = :pagado) " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin) " +
           "AND (:search IS NULL OR :search = '' OR (" +
           "   LOWER(p.cliente.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(p.cliente.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.nombreServicio) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))")
    Page<Pago> findAllWithFilters(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("pagado") Boolean pagado,
            @Param("search") String search,
            Pageable pageable);

    // ==========================================
    // CÁLCULO DE KPIS AGREGADOS EN BASE DE DATOS
    // ==========================================

    // 1. KPI: Total Cobrado y Recuento de Ventas (Rango Dinámico)
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p " +
           "WHERE p.pagado = true " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Double sumTotalCobrado(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(p) FROM Pago p " +
           "WHERE p.pagado = true " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Long countTotalCobrado(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    // 2. KPI: Efectivo en Rango Dinámico
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p " +
           "WHERE p.pagado = true " +
           "AND p.metodoPago = com.example.quiropracticoapi.model.enums.MetodoPago.efectivo " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Double sumEfectivoRango(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(p) FROM Pago p " +
           "WHERE p.pagado = true " +
           "AND p.metodoPago = com.example.quiropracticoapi.model.enums.MetodoPago.efectivo " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Long countEfectivoRango(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    // 3. KPI: Ventas en Bonos (Rango Dinámico)
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p " +
           "LEFT JOIN p.servicioPagado s " +
           "WHERE p.pagado = true " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin) " +
           "AND (s.tipo = com.example.quiropracticoapi.model.enums.TipoServicio.bono " +
           "     OR LOWER(s.nombreServicio) LIKE '%bono%')")
    Double sumVentasBonos(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(p) FROM Pago p " +
           "LEFT JOIN p.servicioPagado s " +
           "WHERE p.pagado = true " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin) " +
           "AND (s.tipo = com.example.quiropracticoapi.model.enums.TipoServicio.bono " +
           "     OR LOWER(s.nombreServicio) LIKE '%bono%')")
    Long countVentasBonos(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    // 4. KPI: Deuda Pendiente (Global y por Rango Dinámico)
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.pagado = false")
    Double sumDeudaPendienteGlobal();

    @Query("SELECT COUNT(p) FROM Pago p WHERE p.pagado = false")
    Long countDeudaPendienteGlobal();

    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p " +
           "WHERE p.pagado = false " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Double sumDeudaPendiente(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(p) FROM Pago p " +
           "WHERE p.pagado = false " +
           "AND (:fechaInicio IS NULL OR p.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR p.fechaCreacion <= :fechaFin)")
    Long countDeudaPendiente(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    // Compatibilidad legacy
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.pagado = true AND p.fechaCreacion BETWEEN :inicio AND :fin")
    Double sumTotalCobradoEnRango(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.pagado = false")
    Double sumTotalPendienteGlobal();
}
