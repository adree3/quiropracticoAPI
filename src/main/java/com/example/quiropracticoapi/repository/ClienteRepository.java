package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findBytelefono (String telefono);

    Optional<Cliente> findByEmail (String email);

    List<Cliente> findByApellidosContainingIgnoreCase(String textoApellidos);}
