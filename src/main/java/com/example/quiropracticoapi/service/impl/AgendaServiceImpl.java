package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;
import com.example.quiropracticoapi.exception.AgendaConflictException;
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
    public BloqueoAgendaDto crearBloqueo(BloqueoAgendaDto dto, boolean force, boolean undo) {
        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la de inicio.");
        }

        BloqueoAgenda bloqueo = new BloqueoAgenda();
        bloqueo.setFechaHoraInicio(dto.getFechaInicio());
        bloqueo.setFechaHoraFin(dto.getFechaFin());
        bloqueo.setMotivo(dto.getMotivo());

        String afectado;

        if (dto.getIdQuiropractico() != null) {
            Usuario quiro = usuarioRepository.findById(dto.getIdQuiropractico())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));
            bloqueo.setUsuario(quiro);
            afectado = quiro.getUsername();

            List<BloqueoAgenda> cierresGlobales = bloqueoAgendaRepository.findBloqueosClinicaQueSolapan(
                    dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!cierresGlobales.isEmpty()) {
                throw new AgendaConflictException(
                        "Ya existe un cierre global en estas fechas.",
                        "CONFLICTO_CIERRE_GLOBAL"
                );
            }

            List<BloqueoAgenda> bloqueosPropios = bloqueoAgendaRepository.findBloqueosUsuarioQueSolapan(
                    dto.getIdQuiropractico(), dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!bloqueosPropios.isEmpty()) {
                throw new AgendaConflictException(
                        "El doctor ya tiene un bloqueo en estas fechas.",
                        "BLOQUEO_DUPLICADO"
                );
            }

            List<Cita> citasConflicto = citaRepository.findCitasConflictivas(
                    dto.getIdQuiropractico(), dto.getFechaInicio(), dto.getFechaFin(), null
            );
            if (!citasConflicto.isEmpty()) {
                int num = citasConflicto.size();
                String msg = (num == 1)
                        ? "El doctor tiene 1 cita agendada en ese periodo."
                        : "El doctor tiene " + num + " citas agendadas en ese periodo.";
                throw new AgendaConflictException(msg, "CITAS_EXISTENTES");
            }
        }else {
            afectado = "CLÍNICA (CIERRE GLOBAL)";

            List<BloqueoAgenda> cierresExistentes = bloqueoAgendaRepository.findBloqueosClinicaQueSolapan(
                    dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!cierresExistentes.isEmpty()) {
                throw new AgendaConflictException(
                        "Ya existe un cierre global registrado en estas fechas.",
                        "BLOQUEO_GLOBAL_DUPLICADO"
                );
            }
            List<BloqueoAgenda> bloqueosIndividuales = bloqueoAgendaRepository.findCualquierBloqueoIndividualQueSolape(
                    dto.getFechaInicio(), dto.getFechaFin()
            );

            if (!bloqueosIndividuales.isEmpty()) {
                if (!force) {
                    int num = bloqueosIndividuales.size();
                    String msg = (num == 1)
                            ? "Existe 1 bloqueo de un doctor en estas fechas."
                            : "Existen " + num + " bloqueos de doctores en estas fechas.";
                    throw new AgendaConflictException(msg, "CONFLICTO_BLOQUEO_INDIVIDUAL");
                } else {
                    bloqueoAgendaRepository.deleteAll(bloqueosIndividuales);

                    int num = bloqueosIndividuales.size();
                    String msgAudit = (num == 1)
                            ? "Se ha eliminado 1 bloqueo individual al crear cierre global."
                            : "Se han eliminado " + num + " bloqueos individuales al crear cierre global.";
                    auditoriaServiceImpl.registrarAccion(
                            TipoAccion.ELIMINAR_FISICO, "BLOQUEO_AGENDA", "VARIOS", msgAudit
                    );
                }
            }

            List<Cita> citasGlobales = citaRepository.findCitasConflictivasGlobales(
                    dto.getFechaInicio(), dto.getFechaFin()
            );
            if (!citasGlobales.isEmpty()) {
                int num = citasGlobales.size();
                String msg = (num == 1)
                        ? "No se puede cerrar la clínica: Hay 1 cita agendada."
                        : "No se puede cerrar la clínica: Hay " + num + " citas agendadas.";
                throw new AgendaConflictException(msg, "CITAS_EXISTENTES_GLOBAL");
            }
        }

        BloqueoAgenda guardado = bloqueoAgendaRepository.save(bloqueo);
        TipoAccion tipo = TipoAccion.CREAR;
        String detalle = "Afectado: " + afectado + ". Motivo: " + guardado.getMotivo();
        if (undo) {
            tipo = TipoAccion.DESHACER;
            detalle = "[UNDO] " + detalle;
        }
        auditoriaServiceImpl.registrarAccion(
                tipo, "BLOQUEO_AGENDA", guardado.getIdBloqueo().toString(),
                 detalle+ " (" + guardado.getFechaHoraInicio() + " - " + guardado.getFechaHoraFin() + ")"
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
    public void borrarBloqueo(Integer id, boolean undo) {
        BloqueoAgenda b = bloqueoAgendaRepository.findById(id).orElse(null);
        bloqueoAgendaRepository.deleteById(id);
        if (b != null) {
            String afectado = (b.getUsuario() != null) ?  b.getUsuario().getUsername() : "CLÍNICA";
            TipoAccion tipo = TipoAccion.ELIMINAR_FISICO;
            String mensaje = "Bloqueo eliminado";
            if (undo) {
                tipo = TipoAccion.DESHACER;
                mensaje = "[UNDO] " + mensaje + " por error";
            }
            auditoriaServiceImpl.registrarAccion(
                    tipo,
                    "BLOQUEO_AGENDA",
                    id.toString(),
                    mensaje+ ": " + afectado + ". Motivo original: " + b.getMotivo() +
                            ". Fechas: " + b.getFechaHoraInicio() + " - " + b.getFechaHoraFin()
            );
        }
    }

    @Override
    public BloqueoAgendaDto actualizarBloqueo(Integer id, BloqueoAgendaDto dto, boolean undo) {
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
            if (!conflictos.isEmpty()) throw new AgendaConflictException("Ya existe otro bloqueo en estas fechas.", "BLOQUEO_DUPLICADO");

            Usuario u = usuarioRepository.findById(dto.getIdQuiropractico()).orElseThrow();
            bloqueo.setUsuario(u);
            detallesCambio += ". Asignado a " + u.getUsername();
        } else {
            List<BloqueoAgenda> conflictos = bloqueoAgendaRepository.findConflictoGlobalExcluyendoId(
                    dto.getFechaInicio(), dto.getFechaFin(), id
            );
            if (!conflictos.isEmpty()) throw new AgendaConflictException("Ya hay un cierre global en estas fechas.", "CONFLICTO_CIERRE_GLOBAL");
            bloqueo.setUsuario(null);
            detallesCambio += ". Asignado a CLÍNICA";
        }

        bloqueo.setFechaHoraInicio(dto.getFechaInicio());
        bloqueo.setFechaHoraFin(dto.getFechaFin());
        bloqueo.setMotivo(dto.getMotivo());
        BloqueoAgenda guardado = bloqueoAgendaRepository.save(bloqueo);

        TipoAccion tipo = TipoAccion.EDITAR;
        String mensaje = "Bloqueo actualizado. " + detallesCambio;

        if (undo) {
            tipo = TipoAccion.DESHACER;
            mensaje = "[UNDO] Se restauraron los valores anteriores " + detallesCambio;
        }
        auditoriaServiceImpl.registrarAccion(
                tipo,
                "BLOQUEO_AGENDA",
                guardado.getIdBloqueo().toString(),
                mensaje+ " (" + guardado.getFechaHoraInicio() + " - " + guardado.getFechaHoraFin() +
                        ")"
        );
        return mapBloqueoToDto(guardado);
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
