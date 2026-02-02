package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;

import java.time.LocalDate;
import java.util.List;

public interface AgendaService {
    BloqueoAgendaDto crearBloqueo(BloqueoAgendaDto dto, boolean force, boolean undo);

    List<BloqueoAgendaDto> getAllBloqueos();

    void borrarBloqueo(Integer id, boolean undo);

    BloqueoAgendaDto actualizarBloqueo(Integer id, BloqueoAgendaDto dto, boolean undo);
}
