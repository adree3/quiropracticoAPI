package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.GrupoFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoFamiliarRepository extends JpaRepository<GrupoFamiliar, Integer> {
    List<GrupoFamiliar> findByPropietarioIdCliente(Integer idPropietario);

    // Ver quién me autoriza a mí (ej. De quién puede gastar el Hijo)
    List<GrupoFamiliar> findByBeneficiarioIdCliente(Integer idBeneficiario);

    boolean existsByPropietarioIdClienteAndBeneficiarioIdCliente(Integer idPropietario, Integer idBeneficiario);
}