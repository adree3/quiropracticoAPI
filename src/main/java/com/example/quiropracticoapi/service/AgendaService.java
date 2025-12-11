package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;

import java.util.List;

public interface AgendaService {
    BloqueoAgendaDto crearBloqueo(BloqueoAgendaDto dto);

    List<BloqueoAgendaDto> getBloqueosFuturos();

    void borrarBloqueo(Integer id);
}
