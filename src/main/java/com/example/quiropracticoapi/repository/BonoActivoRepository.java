package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.BonoActivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonoActivoRepository extends JpaRepository<BonoActivo, Integer> {
    /**
     * Busca todos los bonos del cliente indicado
     * @param clienteId identificador del cliente
     * @return lista de bonos, gastados o no
     */
    List<BonoActivo> findByClienteIdCliente(Integer clienteId);
    /**
     * Busca los bonos de un cliente según las sesiones que tenga
     * @param clienteId indetificador del cliente
     * @param sesiones numero de sesiones (normalmente 0)
     * @return lista de bonos con x sesiones
     */
    List<BonoActivo> findByClienteIdClienteAndSesionesRestantesGreaterThan(Integer clienteId, int sesiones);

    /**
     * Busca bonos disponibles para un cliente, suyos o de algun familiar
     * @param clienteId identificador del cliente
     * @return lista de bonos disponibles
     */
    @Query("SELECT b FROM BonoActivo b " +
            "LEFT JOIN GrupoFamiliar g ON b.cliente.idCliente = g.propietario.idCliente " +
            "WHERE b.sesionesRestantes > 0 " +
            "AND (b.cliente.idCliente = :clienteId OR g.beneficiario.idCliente = :clienteId) " +
            "ORDER BY b.fechaCompra ASC")
    List<BonoActivo> findBonosDisponiblesParaCliente(@Param("clienteId") Integer clienteId);
}

