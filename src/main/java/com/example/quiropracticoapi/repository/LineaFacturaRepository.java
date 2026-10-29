package com.example.quiropracticoapi.repository;

import com.example.quiropracticoapi.model.LineaFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineaFacturaRepository extends JpaRepository<LineaFactura, Integer> {
    List<LineaFactura> findByFacturaId(Integer facturaId);
}
