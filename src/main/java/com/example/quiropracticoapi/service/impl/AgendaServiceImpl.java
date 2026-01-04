package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.BloqueoAgenda;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BloqueoAgendaRepository;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import com.example.quiropracticoapi.service.AgendaService;
import com.example.quiropracticoapi.service.AuditoriaService;
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
    private final AuditoriaServiceImpl auditoriaServiceImpl;


    @Autowired
    public AgendaServiceImpl(UsuarioRepository usuarioRepository, CitaRepository citaRepository, BloqueoAgendaRepository bloqueoAgendaRepository, AuditoriaServiceImpl auditoriaServiceImpl) {
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
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

        String afectado = "CLÍNICA (CIERRE GLOBAL)";

        if (dto.getIdQuiropractico() != null) {
            Usuario quiro = usuarioRepository.findById(dto.getIdQuiropractico())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));
            bloqueo.setUsuario(quiro);

            afectado = quiro.getUsername();

            List<BloqueoAgenda> cierresGlobales = bloqueoAgendaRepository.findBloqueosClinicaQueSolapan(
                    dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!cierresGlobales.isEmpty()) {
                throw new IllegalArgumentException("Ya existe un cierre global en esas fechas.");
            }
            List<Cita> citasConflicto = citaRepository.findCitasConflictivas(
                    dto.getIdQuiropractico(), dto.getFechaInicio(), dto.getFechaFin(), null
            );
            if (!citasConflicto.isEmpty()) {
                throw new IllegalArgumentException("No se puede bloquear: El doctor tiene " + citasConflicto.size() + " citas agendadas en ese periodo.");
            }
        }else {
            List<BloqueoAgenda> cierresExistentes = bloqueoAgendaRepository.findBloqueosClinicaQueSolapan(
                    dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!cierresExistentes.isEmpty()) {
                throw new IllegalArgumentException("Ya existe un cierre de clínica registrado.");
            }
            List<Cita> citasGlobales = citaRepository.findCitasConflictivasGlobales(
                    dto.getFechaInicio(), dto.getFechaFin()
            );

            if (!citasGlobales.isEmpty()) {
                throw new IllegalArgumentException("No se puede cerrar la clínica: Hay " + citasGlobales.size() + " citas agendadas en total durante ese periodo.");
            }
        }

        BloqueoAgenda guardado = bloqueoAgendaRepository.save(bloqueo);
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "BLOQUEO_AGENDA",
                guardado.getIdBloqueo().toString(),
                "Afectado: " + afectado + ". Motivo: " + guardado.getMotivo() +
                        ". Desde: " + guardado.getFechaHoraInicio() + " Hasta: " + guardado.getFechaHoraFin()
        );
        return mapBloqueoToDto(guardado);
    }

    @Override
    public List<BloqueoAgendaDto> getAllBloqueos() {
        return bloqueoAgendaRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "fechaHoraInicio"))
                .stream()
                .map(this::mapBloqueoToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void borrarBloqueo(Integer id) {
        BloqueoAgenda b = bloqueoAgendaRepository.findById(id).orElse(null);
        bloqueoAgendaRepository.deleteById(id);
        if (b != null) {
            String afectado = (b.getUsuario() != null) ?  b.getUsuario().getUsername() : "CLÍNICA";
            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.ELIMINAR_FISICO,
                    "BLOQUEO_AGENDA",
                    id.toString(),
                    "Bloqueo eliminado para: " + afectado + ". Motivo original: " + b.getMotivo() +
                            ". Fechas: " + b.getFechaHoraInicio() + " - " + b.getFechaHoraFin()
            );
        }
    }

    @Override
    public BloqueoAgendaDto actualizarBloqueo(Integer id, BloqueoAgendaDto dto) {
        BloqueoAgenda bloqueo = bloqueoAgendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));

        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new IllegalArgumentException("Fecha fin no puede ser anterior a inicio");
        }

        String detallesCambio = "Motivo: " + dto.getMotivo();

        if (dto.getIdQuiropractico() != null) {
            List<BloqueoAgenda> conflictos = bloqueoAgendaRepository.findConflictoUsuarioExcluyendoId(
                    dto.getIdQuiropractico(), dto.getFechaInicio(), dto.getFechaFin(), id
            );
            if (!conflictos.isEmpty()) throw new IllegalArgumentException("Ya existe otro bloqueo en esas fechas.");

            Usuario u = usuarioRepository.findById(dto.getIdQuiropractico()).orElseThrow();
            bloqueo.setUsuario(u);
            detallesCambio += ". Asignado a " + u.getUsername();
        } else {
            List<BloqueoAgenda> conflictos = bloqueoAgendaRepository.findConflictoGlobalExcluyendoId(
                    dto.getFechaInicio(), dto.getFechaFin(), id
            );
            if (!conflictos.isEmpty()) throw new IllegalArgumentException("Ya hay un cierre global en esas fechas.");
            bloqueo.setUsuario(null);
            detallesCambio += ". Asignado a CLÍNICA";
        }

        bloqueo.setFechaHoraInicio(dto.getFechaInicio());
        bloqueo.setFechaHoraFin(dto.getFechaFin());
        bloqueo.setMotivo(dto.getMotivo());
        BloqueoAgenda guardado = bloqueoAgendaRepository.save(bloqueo);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "BLOQUEO_AGENDA",
                guardado.getIdBloqueo().toString(),
                "Bloqueo reprogramado. Nuevas fechas: " + guardado.getFechaHoraInicio() + " - " + guardado.getFechaHoraFin() +
                        ". " + detallesCambio
        );
        return mapBloqueoToDto(bloqueoAgendaRepository.save(bloqueo));
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
