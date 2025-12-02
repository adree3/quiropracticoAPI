package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.ServicioRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import com.example.quiropracticoapi.repository.ServicioRepository;
import com.example.quiropracticoapi.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;

    @Autowired
    public ServicioServiceImpl(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    @Override
    public List<Servicio> getAllServicios(Boolean activo) {
        if (activo == null) {
            return servicioRepository.findAll();
        }
        return servicioRepository.findByActivo(activo);
    }

    @Override
    public Servicio createServicio(ServicioRequestDto request) {
        Servicio servicio = new Servicio();
        mapDtoToEntity(request, servicio);
        servicio.setActivo(true);
        return servicioRepository.save(servicio);
    }

    @Override
    public Servicio updateServicio(Integer id, ServicioRequestDto request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        mapDtoToEntity(request, servicio);
        return servicioRepository.save(servicio);
    }

    @Override
    public void deleteServicio(Integer id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    @Override
    public void recoverServicio(Integer id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        servicio.setActivo(true);
        servicioRepository.save(servicio);
    }

    private void mapDtoToEntity(ServicioRequestDto dto, Servicio entity) {
        entity.setNombreServicio(dto.getNombreServicio());
        entity.setPrecio(dto.getPrecio());
        try {
            entity.setTipo(TipoServicio.valueOf(dto.getTipo().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: " + dto.getTipo());
        }

        if (entity.getTipo() == TipoServicio.bono) {
            if (dto.getSesionesIncluidas() == null || dto.getSesionesIncluidas() < 1) {
                throw new IllegalArgumentException("Un bono debe tener al menos 1 sesión");
            }
            entity.setSesionesIncluidas(dto.getSesionesIncluidas());
        } else {
            entity.setSesionesIncluidas(null);
        }
    }
}
