package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.ServicioRequestDto;
import com.example.quiropracticoapi.model.Servicio;

import java.util.List;

public interface ServicioService {
    List<Servicio> getAllServicios(Boolean activo);
    Servicio createServicio(ServicioRequestDto request);
    Servicio updateServicio(Integer id, ServicioRequestDto request);
    void deleteServicio(Integer id);
    void recoverServicio(Integer id);
}
