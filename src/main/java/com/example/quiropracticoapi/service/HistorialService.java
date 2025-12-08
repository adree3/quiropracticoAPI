package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.HistorialDto;

public interface HistorialService {
    HistorialDto getHistorialPorCita(Integer idCita);
    HistorialDto saveHistorial(HistorialDto dto);
}
