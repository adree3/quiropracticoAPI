package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.dto.ConsumoBonoDto;
import com.example.quiropracticoapi.dto.BonoHistoricoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


public interface BonoService {

    List<BonoSeleccionDto> getBonosUsables(Integer idCliente);

    void devolverSesion(Integer idBonoActivo);
    void devolverSesion(Integer idBonoActivo, Integer idCita);
    void consumirSesion(Integer idBonoActivo);
    void consumirSesion(Integer idBonoActivo, Integer idCita);

    List<ConsumoBonoDto> getHistorialBono(Integer idBonoActivo);

    Page<BonoHistoricoDto> getHistorialBonos(String search, Pageable pageable);
}
