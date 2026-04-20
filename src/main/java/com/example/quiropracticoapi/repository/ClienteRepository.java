package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.dto.ClienteDetalleProjection;
import com.example.quiropracticoapi.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    /**
     * Devuelve un page de clientes que este activos
     * @param pageable por el numero de pagina que inicia
     * @return page de cientes
     */
    Page<Cliente> findByActivoTrue(Pageable pageable);

    /**
     * Busca el cliente por el telefono indicado
     * @param telefono del cliente
     * @return un cliente
     */
    Optional<Cliente> findBytelefono (String telefono);

    /**
     * Busca un cliente por el correo indicado
     * @param email del cliente
     * @return un cliente
     */
    Optional<Cliente> findByEmail (String email);

    /**
     * Busca por nombres, apellido o telefono
     * @param texto información por la cual buscar
     * @return una lista de clientes filtrados
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "c.activo = true AND " +
            "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "c.telefono LIKE CONCAT('%', :texto, '%')")
    List<Cliente> searchGlobal(@Param("texto") String texto);

    /**
     * Lo mimo que el anterior pero ordenado alfabeticamente y por paginación
     * @param texto información por el cual filtrar
     * @param pageable la paginación
     * @return devuelve un page de clientes filtrado
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "c.activo = true AND " +
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "c.telefono LIKE CONCAT('%', :texto, '%')) " +
            "ORDER BY c.nombre ASC, c.apellidos ASC")
    Page<Cliente> searchGlobalPaged(@Param("texto") String texto, Pageable pageable);

    /**
     * Query unificada y flexible para búsqueda con múltiples filtros opcionales
     * @param activo true/false para filtrar por estado, null para todos
     * @param texto búsqueda global por nombre, apellido o teléfono, null o vacío para omitir
     * @param ultimaCitaDesde fecha mínima de última cita completada, null para omitir
     * @param pageable paginación y ordenamiento
     * @return page de clientes que cumplen los criterios
     */
    @Query("SELECT DISTINCT c FROM Cliente c " +
           "WHERE " +
           "(:activo IS NULL OR c.activo = :activo) AND " +
           "(:texto IS NULL OR :texto = '' OR " +
           "  LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "  LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "  c.telefono LIKE CONCAT('%', :texto, '%')) AND " +
           "(:ultimaCitaDesde IS NULL OR EXISTS (" +
           "  SELECT 1 FROM Cita ct WHERE ct.cliente.idCliente = c.idCliente " +
           "  AND ct.estado = 'completada' AND ct.fechaHoraInicio >= :ultimaCitaDesde))")
    Page<Cliente> findClientesFiltered(
        @Param("activo") Boolean activo,
        @Param("texto") String texto,
        @Param("ultimaCitaDesde") LocalDateTime ultimaCitaDesde,
        Pageable pageable
    );

    /**
     * Busca un cliente por estado
     * @param activo true o false
     * @param pageable de clientes
     * @return una page de clientes segun si esta activo
     */
    Page<Cliente> findByActivo(Boolean activo, Pageable pageable);

    /**
     * Versión optimizada que trae datos agregados (conteo de citas, bonos, etc.) 
     * en una sola consulta para evitar problemas de rendimiento N+1.
     */
    @Query(value = "SELECT c.id_cliente as idCliente, c.nombre as nombre, c.apellidos as apellidos, " +
           "c.email as email, c.telefono as telefono, c.activo as activo, c.fecha_alta as fecha_alta, " +
           "(SELECT COUNT(*) FROM citas ct WHERE ct.id_cliente = c.id_cliente AND ct.estado = 'programada') as countCitasPendientes, " +
           "(SELECT COUNT(*) FROM bonos_activos b WHERE b.id_cliente = c.id_cliente AND b.sesiones_restantes > 0) as countBonosActivos, " +
           "CAST(EXISTS(SELECT 1 FROM grupos_familiares g WHERE g.id_cliente_propietario = c.id_cliente) AS SIGNED) as tieneFamiliares, " +
           "(SELECT MAX(ct2.fecha_hora_inicio) FROM citas ct2 WHERE ct2.id_cliente = c.id_cliente AND ct2.estado = 'completada') as ultimaCita " +
           "FROM clientes c " +
           "WHERE (:activo IS NULL OR c.activo = :activo) " +
           "AND (:texto IS NULL OR :texto = '' OR c.nombre LIKE %:texto% OR c.apellidos LIKE %:texto% OR c.telefono LIKE %:texto%)",
           countQuery = "SELECT COUNT(*) FROM clientes c " +
                        "WHERE (:activo IS NULL OR c.activo = :activo) " +
                        "AND (:texto IS NULL OR :texto = '' OR c.nombre LIKE %:texto% OR c.apellidos LIKE %:texto% OR c.telefono LIKE %:texto%)",
           nativeQuery = true)
    Page<ClienteDetalleProjection> findClientesOptimized(
        @Param("activo") Boolean activo,
        @Param("texto") String texto,
        Pageable pageable
    );

}
