package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Clinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicaRepository extends JpaRepository<Clinica, Long> {
    List<Clinica> findByNombreContainingIgnoreCaseAndActivaTrue(String query);
}
