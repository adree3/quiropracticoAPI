package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;

import java.time.LocalDate;
import java.util.List;

public interface AgendaService {
    BloqueoAgendaDto crearBloqueo(BloqueoAgendaDto dto);

    List<BloqueoAgendaDto> getAllBloqueos();

    void borrarBloqueo(Integer id);

    BloqueoAgendaDto actualizarBloqueo(Integer id, BloqueoAgendaDto dto);
}
