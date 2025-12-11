package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.BloqueoAgenda;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.repository.BloqueoAgendaRepository;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import com.example.quiropracticoapi.service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendaServiceImpl implements AgendaService {

    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;

    @Autowired
    public AgendaServiceImpl(UsuarioRepository usuarioRepository, CitaRepository citaRepository, BloqueoAgendaRepository bloqueoAgendaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
    }

    @Override
    public BloqueoAgendaDto crearBloqueo(BloqueoAgendaDto dto) {
        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la de inicio.");
        }

        BloqueoAgenda bloqueo = new BloqueoAgenda();
        bloqueo.setFechaHoraInicio(dto.getFechaInicio());
        bloqueo.setFechaHoraFin(dto.getFechaFin());
        bloqueo.setMotivo(dto.getMotivo());

        if (dto.getIdQuiropractico() != null) {
            Usuario quiro = usuarioRepository.findById(dto.getIdQuiropractico())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));
            bloqueo.setUsuario(quiro);
        }

        if (dto.getIdQuiropractico() != null) {
            List<Cita> citasConflicto = citaRepository.findCitasConflictivas(
                    dto.getIdQuiropractico(), dto.getFechaInicio(), dto.getFechaFin(), null
            );
            if (!citasConflicto.isEmpty()) {
                throw new IllegalArgumentException("No se puede bloquear: El doctor tiene " + citasConflicto.size() + " citas agendadas en ese periodo.");
            }
        }else {
            List<Cita> citasGlobales = citaRepository.findCitasConflictivasGlobales(
                    dto.getFechaInicio(), dto.getFechaFin()
            );

            if (!citasGlobales.isEmpty()) {
                throw new IllegalArgumentException("No se puede cerrar la clínica: Hay " + citasGlobales.size() + " citas agendadas en total durante ese periodo.");
            }
        }

        BloqueoAgenda guardado = bloqueoAgendaRepository.save(bloqueo);
        return mapBloqueoToDto(guardado);
    }

    @Override
    public List<BloqueoAgendaDto> getBloqueosFuturos() {
        return bloqueoAgendaRepository.findByFechaHoraInicioAfterOrderByFechaHoraInicio(LocalDateTime.now().minusDays(1))
                .stream().map(this::mapBloqueoToDto).collect(Collectors.toList());
    }

    @Override
    public void borrarBloqueo(Integer id) {
        bloqueoAgendaRepository.deleteById(id);
    }

    private BloqueoAgendaDto mapBloqueoToDto(BloqueoAgenda b) {
        BloqueoAgendaDto dto = new BloqueoAgendaDto();
        dto.setIdBloqueo(b.getIdBloqueo());
        dto.setFechaInicio(b.getFechaHoraInicio());
        dto.setFechaFin(b.getFechaHoraFin());
        dto.setMotivo(b.getMotivo());

        if (b.getUsuario() != null) {
            dto.setIdQuiropractico(b.getUsuario().getIdUsuario());
            dto.setNombreQuiropractico(b.getUsuario().getNombreCompleto());
        } else {
            dto.setNombreQuiropractico("TODA LA CLÍNICA");
        }
        return dto;
    }
}
