package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    Optional<Usuario> findByUsernameAndClinicaIdClinica(String username, Long clinicaId);

    Optional<Usuario> findByUsernameAndRol(String username, Rol rol);

    /**
     * Busca el usuario según su rol (quiro, recepcionista o admin)
     * @param rol parametro por el que se busca
     * @return lista de usuarios del mismo rol
     */
    List<Usuario> findByRol(Rol rol);
    List<Usuario> findByRolAndClinicaIdClinica(Rol rol, Long clinicaId);

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
    long countByCuentaBloqueadaTrueAndActivoTrue();

    /**
     * Busca lo usuarios quiropracticos activos
     * @return lista de usuarios
     */
    @Query("SELECT u FROM Usuario u WHERE u.rol = 'quiropráctico' AND u.activo = true AND u.clinica.idClinica = :clinicaId")
    List<Usuario> findQuiropracticosActivos(@org.springframework.data.repository.query.Param("clinicaId") Long clinicaId);

    /**
     * Comprueba si existe el usuario por su username
     * @param username nombre a comprobar
     * @return true o false
     */
    Boolean existsByUsernameAndClinicaIdClinica(String username, Long clinicaId);

    // Métodos filtrados manualmente por clinicaId (ya que UsuarioRepository está excluido del AOP)
    Page<Usuario> findByRolNotAndClinicaIdClinica(Rol rol, Long clinicaId, Pageable pageable);
    Page<Usuario> findByActivoAndRolNotAndClinicaIdClinica(Boolean activo, Rol rol, Long clinicaId, Pageable pageable);
    long countByCuentaBloqueadaTrueAndActivoTrueAndRolNotAndClinicaIdClinica(Rol rol, Long clinicaId);

    // Métodos que excluyen super_admin
    Page<Usuario> findByRolNot(Rol rol, Pageable pageable);
    Page<Usuario> findByActivoAndRolNot(Boolean activo, Rol rol, Pageable pageable);
    long countByCuentaBloqueadaTrueAndActivoTrueAndRolNot(Rol rol);

    /**
     * Búsqueda global por ID saltándose los filtros de Hibernate (Tenant).
     * Las Native Queries no aplican filtros, lo que lo hace ideal para seguridad.
     */
    @Query(value = "SELECT * FROM usuarios WHERE id_usuario = :id", nativeQuery = true)
    Optional<Usuario> findByIdGlobal(@org.springframework.data.repository.query.Param("id") Integer id);
}
