package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.ServicioRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import com.example.quiropracticoapi.repository.ServicioRepository;
import com.example.quiropracticoapi.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;
    private final AuditoriaService auditoriaService;

    @Autowired
    public ServicioServiceImpl(ServicioRepository servicioRepository, AuditoriaService auditoriaService) {
        this.servicioRepository = servicioRepository;
        this.auditoriaService = auditoriaService;
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
        Servicio guardado = servicioRepository.save(servicio);

        String detalle = "Nueva tarifa: " + guardado.getNombreServicio() + " (" + guardado.getPrecio() + "€)";
        if (guardado.getTipo() == TipoServicio.bono) {
            detalle += ". Bono de " + guardado.getSesionesIncluidas() + " sesiones.";
        } else {
            detalle += ". Sesión suelta.";
        }

        auditoriaService.registrarAccion(
                TipoAccion.CREAR,
                "SERVICIO",
                guardado.getIdServicio().toString(),
                detalle
        );

        return guardado;
    }

    @Override
    public Servicio updateServicio(Integer id, ServicioRequestDto request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        BigDecimal precioAnterior = servicio.getPrecio();
        String nombreAnterior = servicio.getNombreServicio();

        mapDtoToEntity(request, servicio);

        Servicio actualizado = servicioRepository.save(servicio);

        auditoriaService.registrarAccion(
                TipoAccion.EDITAR,
                "SERVICIO",
                id.toString(),
                "Actualización de tarifa. " + nombreAnterior + " -> " + actualizado.getNombreServicio() +
                        ". Precio: " + precioAnterior + "€ -> " + actualizado.getPrecio() + "€"
        );

        return actualizado;
    }

    @Override
    public void deleteServicio(Integer id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        servicio.setActivo(false);
        servicioRepository.save(servicio);
        auditoriaService.registrarAccion(
                TipoAccion.ELIMINAR_LOGICO,
                "SERVICIO",
                id.toString(),
                "Tarifa desactivada (archivada): " + servicio.getNombreServicio()
        );
    }

    @Override
    public void recoverServicio(Integer id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        servicio.setActivo(true);
        servicioRepository.save(servicio);
        auditoriaService.registrarAccion(
                TipoAccion.REACTIVAR,
                "SERVICIO",
                id.toString(),
                "Tarifa reactivada: " + servicio.getNombreServicio()
        );
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
            entity.setSesionesIncluidas(1);
        }
    }
}
