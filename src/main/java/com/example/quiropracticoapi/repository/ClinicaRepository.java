package com.example.quiropracticoapi.repository;
import com.example.quiropracticoapi.model.Clinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ClinicaRepository extends JpaRepository<Clinica, Long> {
    List<Clinica> findByNombreContainingIgnoreCaseAndActivaTrue(String query);

    @Transactional
    @Modifying
    @Query("UPDATE Clinica c SET c.almacenamientoUsadoBytes = c.almacenamientoUsadoBytes + :bytes WHERE c.idClinica = :id")
    void incrementarAlmacenamiento(@Param("id") Long id, @Param("bytes") long bytes);
}
