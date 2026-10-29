package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.SecuenciaFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecuenciaFacturacionRepository extends JpaRepository<SecuenciaFacturacion, Integer> {
    Optional<SecuenciaFacturacion> findByClinicaIdClinicaAndPrefijoAnio(Long clinicaId, String prefijoAnio);
}
