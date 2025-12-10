package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {
    /**
     * Busca el usuario por el username
     * @param username del usuario
     * @return un usuario
     */
    Optional<Usuario> findByUsername (String username);

    /**
     * Busca el usuario según su rol (quiro, recepcionista o admin)
     * @param rol parametro por el que se busca
     * @return lista de usuarios del mismo rol
     */
    List<Usuario> findByRol(Rol rol);

    /**
     * Devuelve un page de usuarios activo o no
      * @param activo boolean para indicar si estan activos
     * @param pageable cantidad de usuarios
     * @return page de usuarios
     */
    Page<Usuario> findByActivo(Boolean activo, Pageable pageable);

    /**
     * Numero de cuentas bloqueadas
     * @return devuelve el numero de cuentas bloqueadas
     */
    long countByCuentaBloqueadaTrue();
}
