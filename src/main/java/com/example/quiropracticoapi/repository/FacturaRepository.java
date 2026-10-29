package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByClinicaIdClinicaAndNumeroFactura(Long clinicaId, String numeroFactura);
    List<Factura> findByClienteIdCliente(Integer clienteId);
}
