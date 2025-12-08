package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import org.springframework.stereotype.Service;

import java.util.List;


public interface BonoService {

    List<BonoSeleccionDto> getBonosUsables(Integer idCliente);

}
