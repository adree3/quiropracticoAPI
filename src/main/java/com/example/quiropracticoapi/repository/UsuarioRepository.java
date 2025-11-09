package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {
    /**
     * Busca el usuario por el username
     * @param username del usuario
     * @return un usuario
     */
    Optional<Usuario> findByUsername (String username);

}
