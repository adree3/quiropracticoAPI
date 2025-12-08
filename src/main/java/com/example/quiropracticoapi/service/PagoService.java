package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoService {

    void registrarVentaBono(VentaBonoRequestDto request);

    List<PagoDto> getPagosEnRango(LocalDateTime inicio, LocalDateTime fin);

    List<PagoDto> getPagosPendientes();

    void confirmarPago(Integer idPago);
}
