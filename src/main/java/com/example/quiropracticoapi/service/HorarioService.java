package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.HorarioDto;
import com.example.quiropracticoapi.dto.HorarioRequestDto;

import java.util.List;

public interface HorarioService {

    List<HorarioDto> getHorariosByQuiropractico(Integer idQuiro);

    HorarioDto createHorario(HorarioRequestDto request);

    void deleteHorario(Integer idHorario);
}
