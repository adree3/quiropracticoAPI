package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.HorarioDto;
import com.example.quiropracticoapi.dto.HorarioRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Horario;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.repository.HorarioRepository;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import com.example.quiropracticoapi.service.HorarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioServiceImpl implements HorarioService {

    private final HorarioRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public HorarioServiceImpl(HorarioRepository horarioRepository, UsuarioRepository usuarioRepository) {
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<HorarioDto> getHorariosByQuiropractico(Integer idQuiro) {
        return horarioRepository.findByQuiropracticoIdUsuario(idQuiro).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HorarioDto createHorario(HorarioRequestDto request) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior al inicio");
        }

        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));

        List<Horario> horariosDelDia = horarioRepository.findByQuiropracticoIdUsuarioAndDiaSemana(
                quiro.getIdUsuario(),
                request.getDiaSemana().byteValue()
        );

        for (Horario existente : horariosDelDia) {
            if (request.getHoraInicio().isBefore(existente.getHoraFin()) &&
                    request.getHoraFin().isAfter(existente.getHoraInicio())) {
                throw new IllegalArgumentException(
                        "El nuevo turno se solapa con uno existente (" +
                                existente.getHoraInicio() + " - " + existente.getHoraFin() + ")"
                );
            }
        }

        Horario horario = new Horario();
        horario.setQuiropractico(quiro);
        horario.setDiaSemana(request.getDiaSemana().byteValue());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        return toDto(horarioRepository.save(horario));
    }

    @Override
    public void deleteHorario(Integer idHorario) {
        if (!horarioRepository.existsById(idHorario)) {
            throw new ResourceNotFoundException("Horario no encontrado");
        }
        horarioRepository.deleteById(idHorario);
    }

    private HorarioDto toDto(Horario h) {
        HorarioDto dto = new HorarioDto();
        dto.setIdHorario(h.getIdHorario());
        dto.setDiaSemana(h.getDiaSemana().intValue());
        dto.setHoraInicio(h.getHoraInicio());
        dto.setHoraFin(h.getHoraFin());
        return dto;
    }
}
