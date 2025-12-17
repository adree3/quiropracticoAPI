package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.HistorialDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.HistorialClinico;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.HistorialClinicoRepository;
import com.example.quiropracticoapi.service.HistorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class HistorialServiceImpl implements HistorialService {


    private final HistorialClinicoRepository historialRepo;
    private final CitaRepository citaRepository;
    private final AuditoriaService auditoriaService;


    @Autowired
    public HistorialServiceImpl(HistorialClinicoRepository historialRepo, CitaRepository citaRepository, AuditoriaService auditoriaService) {
        this.historialRepo = historialRepo;
        this.citaRepository = citaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public HistorialDto getHistorialPorCita(Integer idCita) {
        // Buscamos si ya existe historial para esa cita
        Optional<HistorialClinico> historialOpt = historialRepo.findByCitaIdCita(idCita);

        if (historialOpt.isPresent()) {
            return toDto(historialOpt.get());
        } else {
            HistorialDto vacio = new HistorialDto();
            vacio.setIdCita(idCita);
            return vacio;
        }
    }

    @Override
    @Transactional
    public HistorialDto saveHistorial(HistorialDto dto) {
        HistorialClinico historial;
        boolean esNuevo = false;

        Optional<HistorialClinico> existente = historialRepo.findByCitaIdCita(dto.getIdCita());

        if (existente.isPresent()) {
            historial = existente.get();
        } else {
            esNuevo = true;
            historial = new HistorialClinico();
            Cita cita = citaRepository.findById(dto.getIdCita())
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

            historial.setCita(cita);
            historial.setCliente(cita.getCliente());
            historial.setQuiropractico(cita.getQuiropractico());
            historial.setFechaSesion(LocalDateTime.now());
        }

        historial.setNotasSubjetivo(dto.getNotasSubjetivo());
        historial.setNotasObjetivo(dto.getNotasObjetivo());
        historial.setAjustesRealizados(dto.getAjustesRealizados());
        historial.setPlanFuturo(dto.getPlanFuturo());

        HistorialClinico guardado = historialRepo.save(historial);

        if (esNuevo) {
            auditoriaService.registrarAccion(
                    TipoAccion.CREAR,
                    "HISTORIAL_CLINICO",
                    guardado.getIdHistorial().toString(),
                    "Creación inicial de historial para Cita ID: " + guardado.getCita().getIdCita() +
                            ". Paciente: " + guardado.getCliente().getNombre()
            );
        } else {
            auditoriaService.registrarAccion(
                    TipoAccion.EDITAR,
                    "HISTORIAL_CLINICO",
                    guardado.getIdHistorial().toString(),
                    "Modificación de notas clínicas. Cita ID: " + guardado.getCita().getIdCita() +
                            ". Paciente: " + guardado.getCliente().getNombre()
            );
        }

        return toDto(guardado);
    }

    private HistorialDto toDto(HistorialClinico h) {
        HistorialDto dto = new HistorialDto();
        dto.setIdHistorial(h.getIdHistorial());
        dto.setIdCita(h.getCita().getIdCita());
        dto.setNotasSubjetivo(h.getNotasSubjetivo());
        dto.setNotasObjetivo(h.getNotasObjetivo());
        dto.setAjustesRealizados(h.getAjustesRealizados());
        dto.setPlanFuturo(h.getPlanFuturo());
        return dto;
    }
}