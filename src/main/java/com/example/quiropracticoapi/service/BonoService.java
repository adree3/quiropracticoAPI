package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.dto.ConsumoBonoDto;
import org.springframework.stereotype.Service;

import java.util.List;


public interface BonoService {

    List<BonoSeleccionDto> getBonosUsables(Integer idCliente);

    void devolverSesion(Integer idBonoActivo);
    void devolverSesion(Integer idBonoActivo, Integer idCita);
    void consumirSesion(Integer idBonoActivo);
    void consumirSesion(Integer idBonoActivo, Integer idCita);

    List<ConsumoBonoDto> getHistorialBono(Integer idBonoActivo);
}
