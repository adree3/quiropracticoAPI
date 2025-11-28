package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.VentaBonoRequestDto;

public interface PagoService {

    void registrarVentaBono(VentaBonoRequestDto request);
}
