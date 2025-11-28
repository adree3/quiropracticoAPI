package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
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
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "c.telefono LIKE CONCAT('%', :texto, '%')) " +
            "ORDER BY c.nombre ASC, c.apellidos ASC")
    Page<Cliente> searchGlobalPaged(@Param("texto") String texto, Pageable pageable);
}
