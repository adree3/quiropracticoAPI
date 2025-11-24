package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.CitaMapper;
import com.example.quiropracticoapi.model.*;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.repository.*;
import com.example.quiropracticoapi.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CitaServiceImpl implements CitaService {
    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioRepository horarioRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final CitaMapper citaMapper;
    private final BonoActivoRepository bonoActivoRepository;
    private final ConsumoBonoRepository consumoBonoRepository;


    @Autowired
    public CitaServiceImpl(CitaRepository citaRepository, ClienteRepository clienteRepository, UsuarioRepository usuarioRepository, HorarioRepository horarioRepository, BloqueoAgendaRepository bloqueoAgendaRepository, CitaMapper citaMapper, BonoActivoRepository bonoActivoRepository, ConsumoBonoRepository consumoBonoRepository) {
        this.citaRepository = citaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioRepository = horarioRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.citaMapper = citaMapper;
        this.bonoActivoRepository = bonoActivoRepository;
        this.consumoBonoRepository = consumoBonoRepository;
    }

    @Override
    @Transactional
    public CitaDto crearCita(CitaRequestDto request) {
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));

        // VALIDACIONES DE NEGOCIO
        validarDisponibilidad(quiro, request, null);

        Cita cita = citaMapper.toEntity(request, cliente, quiro);
        Cita citaGuardada = citaRepository.save(cita);

        gestionarConsumoBono(citaGuardada, cliente, request.getIdBonoAUtilizar());

        return citaMapper.toDto(citaGuardada);
    }

    private void validarDisponibilidad(Usuario quiro, CitaRequestDto request, Integer idCitaExcluir) {
        if (request.getFechaHoraFin().isBefore(request.getFechaHoraInicio())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        int diaSemanaJava = request.getFechaHoraInicio().getDayOfWeek().getValue();
        List<Horario> horariosDia = horarioRepository.findByQuiropracticoIdUsuarioAndDiaSemana(
                quiro.getIdUsuario(), (byte) diaSemanaJava
        );

        boolean entraEnHorario = false;
        LocalTime horaInicioCita = request.getFechaHoraInicio().toLocalTime();
        LocalTime horaFinCita = request.getFechaHoraFin().toLocalTime();

        for (Horario h : horariosDia) {
            if (!horaInicioCita.isBefore(h.getHoraInicio()) && !horaFinCita.isAfter(h.getHoraFin())) {
                entraEnHorario = true;
                break;
            }
        }

        if (!entraEnHorario) {
            throw new IllegalArgumentException("El quiropráctico no tiene horario disponible en ese momento.");
        }

        List<BloqueoAgenda> bloqueosPersonales = bloqueoAgendaRepository.findBloqueosPersonalesQueSolapan(
                quiro.getIdUsuario(), request.getFechaHoraInicio(), request.getFechaHoraFin()
        );
        List<BloqueoAgenda> bloqueosClinica = bloqueoAgendaRepository.findBloqueosClinicaQueSolapan(
                request.getFechaHoraInicio(), request.getFechaHoraFin()
        );

        if (!bloqueosPersonales.isEmpty() || !bloqueosClinica.isEmpty()) {
            throw new IllegalArgumentException("El horario seleccionado coincide con un bloqueo de agenda (vacaciones o festivo).");
        }

        List<Cita> citasSolapadas = citaRepository.findCitasConflictivas(
                quiro.getIdUsuario(),
                request.getFechaHoraInicio(),
                request.getFechaHoraFin(),
                idCitaExcluir
        );;

        if (!citasSolapadas.isEmpty()) {
            throw new IllegalArgumentException("Ya existe una cita reservada en ese horario (conflicto de horario).");
        }
    }

    private void gestionarConsumoBono(Cita cita, Cliente cliente, Integer idBonoForzado) {
        BonoActivo bonoAUtilizar = null;

        // Bono elegido
        if (idBonoForzado != null) {
            BonoActivo bono = bonoActivoRepository.findById(idBonoForzado)
                    .orElseThrow(() -> new IllegalArgumentException("El bono seleccionado no existe"));

            List<BonoActivo> bonosPermitidos = bonoActivoRepository.findBonosDisponiblesParaCliente(cliente.getIdCliente());

            boolean tienePermiso = bonosPermitidos.stream()
                    .anyMatch(b -> b.getIdBonoActivo().equals(idBonoForzado));

            if (!tienePermiso) {
                throw new IllegalArgumentException("El cliente no tiene permiso para usar este bono (no es suyo ni familiar).");
            }

            if (bono.getSesionesRestantes() <= 0) {
                throw new IllegalArgumentException("El bono seleccionado no tiene sesiones disponibles.");
            }

            bonoAUtilizar = bono;

        } else {
            // Buscamos el bono más antiguo disponible (propio o de familia)
            List<BonoActivo> bonosDisponibles = bonoActivoRepository
                    .findBonosDisponiblesParaCliente(cliente.getIdCliente());
            if (!bonosDisponibles.isEmpty()) {
                bonoAUtilizar = bonosDisponibles.getFirst();
            }
        }

        // Si hemos encontrado un bono válido, procedemos al cobro
        if (bonoAUtilizar != null) {
            bonoAUtilizar.setSesionesRestantes(bonoAUtilizar.getSesionesRestantes() - 1);
            bonoActivoRepository.save(bonoAUtilizar);
            ConsumoBono consumo = new ConsumoBono();
            consumo.setCita(cita);
            consumo.setBonoActivo(bonoAUtilizar);
            consumoBonoRepository.save(consumo);
        }
    }

    @Override
    public List<CitaDto> getCitasPorFecha(LocalDate fecha) {
        return citaRepository.findByFechaHoraInicioBetween(
                fecha.atStartOfDay(),
                fecha.atTime(23, 59, 59)
        ).stream().map(citaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<CitaDto> getCitasPorCliente(Integer idCliente) {
        return citaRepository.findByClienteIdCliente(idCliente)
                .stream().map(citaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public void cancelarCita(Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        cita.setEstado(EstadoCita.cancelada);
        citaRepository.save(cita);
    }

    @Override
    public CitaDto getCitaById(Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        return citaMapper.toDto(cita);
    }

    @Override
    public CitaDto cambiarEstado(Integer idCita, EstadoCita nuevoEstado) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        cita.setEstado(nuevoEstado);
        Cita citaGuardada = citaRepository.save(cita);
        return citaMapper.toDto(citaGuardada);
    }

    @Override
    @Transactional
    public CitaDto updateCita(Integer idCita, CitaRequestDto request) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        validarDisponibilidad(quiro, request, idCita);

        cita.setQuiropractico(quiro);
        cita.setCliente(cliente);
        cita.setFechaHoraInicio(request.getFechaHoraInicio());
        cita.setFechaHoraFin(request.getFechaHoraFin());
        cita.setNotasRecepcion(request.getNotasRecepcion());

        if (request.getEstado() != null) {
            try {
                cita.setEstado(EstadoCita.valueOf(request.getEstado().toLowerCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Error"+ e.getMessage());
            }
        }

        Cita actualizada = citaRepository.save(cita);
        return citaMapper.toDto(actualizada);
    }
}
