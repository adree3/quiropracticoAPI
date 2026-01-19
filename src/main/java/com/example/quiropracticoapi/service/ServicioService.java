package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.ServicioRequestDto;
import com.example.quiropracticoapi.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServicioService {
    Page<Servicio> getAllServicios(Boolean activo, Pageable pageable);
    List<Servicio> getServiciosParaDropdown();
    Servicio createServicio(ServicioRequestDto request);
    Servicio updateServicio(Integer id, ServicioRequestDto request);
    void deleteServicio(Integer id);
    void recoverServicio(Integer id);
}
