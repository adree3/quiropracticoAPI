package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Busca los clientes que sus apellidos contengan lo indicado
     * @param textoApellidos de los clientes
     * @return lista de clientes
     */
    List<Cliente> findByApellidosContainingIgnoreCase(String textoApellidos);}
