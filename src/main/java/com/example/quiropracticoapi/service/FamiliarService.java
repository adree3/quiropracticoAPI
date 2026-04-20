package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.CitaConflictoDto;

import java.util.List;

public interface FamiliarService {
    List<CitaConflictoDto> obtenerCitasConflictivas(Integer idFamiliar);

    void desvincularFamiliar(Integer idGrupo, List<Integer> idsCitasACancelar);
}
