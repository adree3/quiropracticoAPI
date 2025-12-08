package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.dto.HuecoDto;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
            }else {
                throw new IllegalArgumentException("El cliente NO tiene bonos activos con saldo.");
            }
        }

        if (bonoAUtilizar != null) {
            int nuevoSaldo = bonoAUtilizar.getSesionesRestantes() - 1;
            bonoAUtilizar.setSesionesRestantes(nuevoSaldo);
            bonoActivoRepository.save(bonoAUtilizar);

            ConsumoBono consumo = new ConsumoBono();
            consumo.setCita(cita);
            consumo.setBonoActivo(bonoAUtilizar);

            consumo.setSesionesRestantesSnapshot(nuevoSaldo);
            consumoBonoRepository.save(consumo);
        }
    }

    @Override
    public List<CitaDto> getCitasPorFecha(LocalDate fecha) {
        List<Cita> citas=  citaRepository.findByFechaHoraInicioBetween(
                fecha.atStartOfDay(),
                fecha.atTime(23, 59, 59)
        );
        return citas.stream().map(c -> {
            CitaDto dto = citaMapper.toDto(c);
            llenarInfoPago(c, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CitaDto> getCitasPorCliente(Integer idCliente) {
        return citaRepository.findByClienteIdClienteOrderByFechaHoraInicioDesc(idCliente)
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
        CitaDto dto = citaMapper.toDto(cita);
        llenarInfoPago(cita,dto);

        return dto;
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

    @Override
    public List<HuecoDto> getHuecosDisponibles(Integer idQuiro, LocalDate fecha, Integer idCitaExcluir) {
        List<HuecoDto> huecosLibres = new ArrayList<>();
        int duracionCitaMinutos = 30;

        int diaSemana = fecha.getDayOfWeek().getValue();
        List<Horario> horarios = horarioRepository.findByQuiropracticoIdUsuarioAndDiaSemana(idQuiro, (byte) diaSemana);

        if (horarios.isEmpty()) return huecosLibres;


        List<Cita> citasDelDia = citaRepository.findByQuiropracticoIdUsuarioAndFechaHoraInicioBetween(
                idQuiro, fecha.atStartOfDay(), fecha.atTime(23, 59, 59));

        List<BloqueoAgenda> bloqueosDelDia = bloqueoAgendaRepository.findBloqueosPersonalesQueSolapan(
                idQuiro, fecha.atStartOfDay(), fecha.atTime(23, 59, 59));

        for (Horario turno : horarios) {
            LocalTime cursor = turno.getHoraInicio();

            while (cursor.plusMinutes(duracionCitaMinutos).isBefore(turno.getHoraFin()) ||
                    cursor.plusMinutes(duracionCitaMinutos).equals(turno.getHoraFin())) {

                LocalTime finCita = cursor.plusMinutes(duracionCitaMinutos);

                if (esHuecoValido(fecha, cursor, finCita, citasDelDia, bloqueosDelDia, idCitaExcluir)) {
                    huecosLibres.add(new HuecoDto(
                            cursor.toString(),
                            finCita.toString(),
                            cursor + " - " + finCita
                    ));
                }

                cursor = cursor.plusMinutes(duracionCitaMinutos);
            }
        }

        huecosLibres.sort(Comparator.comparing(HuecoDto::getHoraInicio));

        return huecosLibres;
    }

    private boolean esHuecoValido(LocalDate fecha, LocalTime inicio, LocalTime fin, List<Cita> citas, List<BloqueoAgenda> bloqueos, Integer idExcluir) {
        LocalDateTime inicioHueco = LocalDateTime.of(fecha, inicio);
        LocalDateTime finHueco = LocalDateTime.of(fecha, fin);

        for (Cita c : citas) {
            if (c.getIdCita().equals(idExcluir)) {
                continue;
            }
            if (c.getEstado() != EstadoCita.cancelada) {
                if (inicioHueco.isBefore(c.getFechaHoraFin()) && finHueco.isAfter(c.getFechaHoraInicio())) {
                    return false;
                }
            }
        }

        for (BloqueoAgenda b : bloqueos) {
            if (inicioHueco.isBefore(b.getFechaHoraFin()) && finHueco.isAfter(b.getFechaHoraInicio())) {
                return false;
            }
        }
        return true;
    }

    private void llenarInfoPago(Cita cita, CitaDto dto) {
        var consumoOpt = consumoBonoRepository.findByCitaIdCita(cita.getIdCita());

        if (consumoOpt.isPresent()) {
            ConsumoBono consumo = consumoOpt.get();
            BonoActivo bono = consumo.getBonoActivo();
            String nombreBono = bono.getServicioComprado().getNombreServicio();

            int restantesHistorico = consumo.getSesionesRestantesSnapshot() != null
                    ? consumo.getSesionesRestantesSnapshot()
                    : bono.getSesionesRestantes();
            String infoSaldo = " / quedan " + restantesHistorico;

            if (bono.getCliente().getIdCliente().equals(cita.getCliente().getIdCliente())) {
                dto.setInfoPago("Bono Propio (" + nombreBono  + infoSaldo+")");
            } else {
                dto.setInfoPago("Bono de " + bono.getCliente().getNombre() + " (" + nombreBono + infoSaldo + ")");
            }
        } else {
            dto.setInfoPago("Pago Directo / Sesión Suelta");
        }
    }
}
