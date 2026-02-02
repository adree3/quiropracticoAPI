package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.HorarioDto;
import com.example.quiropracticoapi.dto.HorarioGlobalDto;
import com.example.quiropracticoapi.dto.HorarioRequestDto;
import com.example.quiropracticoapi.exception.HorarioOverlapException;
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

    @Override
    public List<HorarioDto> getHorariosByQuiropractico(Integer idQuiro) {
        return horarioRepository.findByQuiropracticoIdUsuario(idQuiro).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HorarioDto createHorario(HorarioRequestDto request) {
        validarLogicaHoras(request);

        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));

        List<Horario> horariosDelDia = horarioRepository.findByQuiropracticoIdUsuarioAndDiaSemana(
                quiro.getIdUsuario(),
                request.getDiaSemana().byteValue()
        );
        String nombreDia = convertirDiaSemana(request.getDiaSemana().byteValue());
        validarConflictos(horariosDelDia, request, null, nombreDia, false);

        Horario horario = new Horario();
        horario.setQuiropractico(quiro);
        horario.setDiaSemana(request.getDiaSemana().byteValue());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        Horario guardado = horarioRepository.save(horario);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR, "HORARIO", guardado.getIdHorario().toString(),
                "Nuevo turno: " + quiro.getNombreCompleto() + " (" + nombreDia + " " + guardado.getHoraInicio() + "-" + guardado.getHoraFin() + ")"
        );
        return toDto(guardado);
    }

    @Override
    @Transactional
    public HorarioDto updateHorario(Integer idHorario, HorarioRequestDto request) {
        validarLogicaHoras(request);

        Horario horario = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        Byte diaOriginal = horario.getDiaSemana();

        Usuario quiro = horario.getQuiropractico();
        if (!quiro.getIdUsuario().equals(request.getIdQuiropractico())) {
            quiro = usuarioRepository.findById(request.getIdQuiropractico())
                    .orElseThrow(() -> new ResourceNotFoundException("Nuevo quiropráctico no encontrado"));
        }

        List<Horario> horariosDelDia = horarioRepository.findByQuiropracticoIdUsuarioAndDiaSemana(
                quiro.getIdUsuario(),
                request.getDiaSemana().byteValue()
        );

        boolean haCambiadoDeDia = !diaOriginal.equals(request.getDiaSemana().byteValue());
        String nombreDia = convertirDiaSemana(request.getDiaSemana().byteValue());

        validarConflictos(horariosDelDia, request, idHorario, nombreDia, haCambiadoDeDia);

        // Actualizar
        horario.setQuiropractico(quiro);
        horario.setDiaSemana(request.getDiaSemana().byteValue());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        Horario actualizado = horarioRepository.save(horario);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR, "HORARIO", actualizado.getIdHorario().toString(),
                "Turno modificado: " + quiro.getNombreCompleto() + " (" + nombreDia + " " + actualizado.getHoraInicio() + "-" + actualizado.getHoraFin() + ")"
        );

        return toDto(actualizado);
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

    private void validarLogicaHoras(HorarioRequestDto request) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new HorarioOverlapException("La hora de fin debe ser posterior al inicio", "HORA");
        }
    }

    /**
     * Lógica centralizada de validación de choques
     */
    private void validarConflictos(List<Horario> existentes, HorarioRequestDto request, Integer idAExcluir, String nombreDia, boolean esCambioDia) {
        for (Horario existente : existentes) {
            if (existente.getIdHorario().equals(idAExcluir)) {
                continue;
            }

            // Comprobar si hay intersección de rangos
            if (request.getHoraInicio().isBefore(existente.getHoraFin()) &&
                    request.getHoraFin().isAfter(existente.getHoraInicio())) {

                if (esCambioDia) {
                    throw new HorarioOverlapException(
                            "El " + nombreDia + " ya tiene un turno que se solapa: " +
                                    existente.getHoraInicio() + " - " + existente.getHoraFin(),
                            "DIA"
                    );
                }

                boolean esExacto = request.getHoraInicio().equals(existente.getHoraInicio()) &&
                        request.getHoraFin().equals(existente.getHoraFin());

                if (esExacto) {
                    throw new HorarioOverlapException(
                            "Ya existe un turno idéntico (" + existente.getHoraInicio() + " - " + existente.getHoraFin() + ") en este día.",
                            "DUPLICADO"
                    );
                }

                throw new HorarioOverlapException(
                        "El " + nombreDia + " ya tiene un turno (" + existente.getHoraInicio() + " - " + existente.getHoraFin() + ") que se solapa con el nuevo horario.",
                        "HORA"
                );
            }
        }
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
