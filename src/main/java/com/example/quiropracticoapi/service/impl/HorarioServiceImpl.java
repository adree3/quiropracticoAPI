package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.HorarioDto;
import com.example.quiropracticoapi.dto.HorarioGlobalDto;
import com.example.quiropracticoapi.dto.HorarioRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Horario;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.TipoAccion;
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
    private final AuditoriaServiceImpl auditoriaServiceImpl;

    @Autowired
    public HorarioServiceImpl(HorarioRepository horarioRepository, UsuarioRepository usuarioRepository, AuditoriaServiceImpl auditoriaServiceImpl) {
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
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

        Horario guardado = horarioRepository.save(horario);

        String diaSemanaStr = convertirDiaSemana(guardado.getDiaSemana());
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "HORARIO",
                guardado.getIdHorario().toString(),
                "Nuevo turno asignado a: " + quiro.getNombreCompleto() +
                        ". Día: " + diaSemanaStr + ". Horario: " + guardado.getHoraInicio() + " - " + guardado.getHoraFin()
        );

        return toDto(guardado);
    }

    @Override
    public void deleteHorario(Integer idHorario) {
        Horario h = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
        horarioRepository.deleteById(idHorario);
        String diaSemanaStr = convertirDiaSemana(h.getDiaSemana());
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.ELIMINAR_FISICO,
                "HORARIO",
                idHorario.toString(),
                "Turno eliminado de: " + h.getQuiropractico().getNombreCompleto() +
                        ". Era el día: " + diaSemanaStr + " de " + h.getHoraInicio() + " a " + h.getHoraFin()
        );
    }

    @Override
    public List<HorarioGlobalDto> getAllHorariosActivos() {
        List<Horario> horarios = horarioRepository.findByQuiropracticoActivoTrue();
        return horarios.stream().map(h -> {
            HorarioGlobalDto dto = new HorarioGlobalDto();
            dto.setIdHorario(h.getIdHorario());
            dto.setIdQuiropractico(h.getQuiropractico().getIdUsuario());
            dto.setDiaSemana(h.getDiaSemana());
            dto.setHoraInicio(h.getHoraInicio());
            dto.setHoraFin(h.getHoraFin());
            return dto;
        }).collect(Collectors.toList());
    }

    private HorarioDto toDto(Horario h) {
        HorarioDto dto = new HorarioDto();
        dto.setIdHorario(h.getIdHorario());
        dto.setDiaSemana(h.getDiaSemana().intValue());
        dto.setHoraInicio(h.getHoraInicio());
        dto.setHoraFin(h.getHoraFin());
        return dto;
    }
    private String convertirDiaSemana(Byte dia) {
        return switch (dia) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + dia;
        };
    }
}
