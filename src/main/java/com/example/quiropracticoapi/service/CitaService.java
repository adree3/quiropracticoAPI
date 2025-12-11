package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.dto.HuecoDto;
import com.example.quiropracticoapi.model.enums.EstadoCita;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {
    CitaDto crearCita(CitaRequestDto request);

    List<CitaDto> getCitasPorFecha(LocalDate fecha);

    List<CitaDto> getCitasPorCliente(Integer idCliente);

    void cancelarCita(Integer idCita);

    CitaDto getCitaById(Integer idCita);

    CitaDto cambiarEstado(Integer idCita, EstadoCita nuevoEstado);

    CitaDto updateCita(Integer idCita, CitaRequestDto request);

    List<HuecoDto> getHuecosDisponibles(Integer idQuiro, LocalDate fecha, Integer idCitaExcluir);


}
