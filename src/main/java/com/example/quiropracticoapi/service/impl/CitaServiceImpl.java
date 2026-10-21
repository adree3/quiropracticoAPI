package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.dto.HuecoDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.CitaMapper;
import com.example.quiropracticoapi.model.*;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.*;
import com.example.quiropracticoapi.service.CitaService;
import com.example.quiropracticoapi.service.impl.R2StorageService;
import com.example.quiropracticoapi.util.StoragePathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
    private final AuditoriaServiceImpl auditoriaServiceImpl;
    private final PdfFirmaService pdfFirmaService;
    private final R2StorageService r2StorageService;
    private final StoragePathBuilder storagePathBuilder;
    private final DocumentoClienteRepository documentoClienteRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    @Autowired
    public CitaServiceImpl(CitaRepository citaRepository, 
                           ClienteRepository clienteRepository, 
                           UsuarioRepository usuarioRepository, 
                           HorarioRepository horarioRepository, 
                           BloqueoAgendaRepository bloqueoAgendaRepository, 
                           CitaMapper citaMapper, 
                           BonoActivoRepository bonoActivoRepository, 
                           ConsumoBonoRepository consumoBonoRepository, 
                           AuditoriaServiceImpl auditoriaServiceImpl, 
                           PdfFirmaService pdfFirmaService,
                           R2StorageService r2StorageService,
                           StoragePathBuilder storagePathBuilder,
                           DocumentoClienteRepository documentoClienteRepository,
                           SimpMessagingTemplate messagingTemplate,
                           org.springframework.context.ApplicationEventPublisher eventPublisher,
                           PlatformTransactionManager transactionManager) {
        this.citaRepository = citaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioRepository = horarioRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.citaMapper = citaMapper;
        this.bonoActivoRepository = bonoActivoRepository;
        this.consumoBonoRepository = consumoBonoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
        this.pdfFirmaService = pdfFirmaService;
        this.r2StorageService = r2StorageService;
        this.storagePathBuilder = storagePathBuilder;
        this.documentoClienteRepository = documentoClienteRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional
    public CitaDto crearCita(CitaRequestDto request) {
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));

        validarDisponibilidad(quiro, request, null);

        Cita cita = citaMapper.toEntity(request, cliente, quiro);
        
        // REGLA 1: Asignar clínica explícitamente desde la entidad padre
        if (cliente.getClinica() != null) {
            cita.setClinica(cliente.getClinica());
        }

        Cita citaGuardada = citaRepository.save(cita);

        BonoActivo bonoUsado = gestionarConsumoBono(citaGuardada, cliente, request.getIdBonoAUtilizar());
        String detallesPago = (bonoUsado != null) ? "Pago con Bono ID: " + bonoUsado.getIdBonoActivo() : "Pago Directo/Pendiente";
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "CITA",
                citaGuardada.getIdCita().toString(),
                "Cliente: " + cliente.getNombre() + ". Quiro: " + quiro.getUsername() +
                        ". Fecha: " + citaGuardada.getFechaHoraInicio() + ". " + detallesPago
        );

        eventPublisher.publishEvent(new com.example.quiropracticoapi.event.CitaCreadaEvent(citaGuardada.getIdCita()));

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

    private BonoActivo gestionarConsumoBono(Cita cita, Cliente cliente, Integer idBonoForzado) {
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

        } else if (cita.getIdBonoPreasignado() != null) {
            // Prioridad a la memoria de la cita
            BonoActivo bonoMemory = bonoActivoRepository.findById(cita.getIdBonoPreasignado()).orElse(null);
            if (bonoMemory != null && bonoMemory.getSesionesRestantes() > 0) {
                bonoAUtilizar = bonoMemory;
            } else {
                // Si el de memoria ya no es válido, buscador normal
                List<BonoActivo> bonosDisponibles = bonoActivoRepository.findBonosDisponiblesParaCliente(cliente.getIdCliente());
                if (!bonosDisponibles.isEmpty()) bonoAUtilizar = bonosDisponibles.getFirst();
            }
        } else {
            // Buscamos el bono más antiguo disponible (propio o de familia)
            List<BonoActivo> bonosDisponibles = bonoActivoRepository
                    .findBonosDisponiblesParaCliente(cliente.getIdCliente());
            if (!bonosDisponibles.isEmpty()) {
                bonoAUtilizar = bonosDisponibles.getFirst();
            } else {
                throw new IllegalArgumentException("El cliente NO tiene bonos activos con saldo.");
            }
        }

        if (bonoAUtilizar != null) {
            // Limpiar la pre-asignación ya que se va a consumir
            cita.setIdBonoPreasignado(null);
            
            int saldoAnterior = bonoAUtilizar.getSesionesRestantes();
            int nuevoSaldo = saldoAnterior - 1;
            bonoAUtilizar.setSesionesRestantes(nuevoSaldo);
            BonoActivo bonoGuardado = bonoActivoRepository.save(bonoAUtilizar);

            ConsumoBono consumo = new ConsumoBono();
            consumo.setCita(cita);
            consumo.setBonoActivo(bonoGuardado);
            consumo.setSesionesRestantesSnapshot(nuevoSaldo);
            if (cita.getClinica() != null) {
                consumo.setClinica(cita.getClinica());
            }
            consumoBonoRepository.save(consumo);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.CONSUMO,
                    "BONO",
                    bonoGuardado.getIdBonoActivo().toString(),
                    "Sesión consumida para Cita ID: " + cita.getIdCita() +
                            ". Cliente: " + cliente.getNombre() + ". Saldo: " + saldoAnterior + " -> " + nuevoSaldo
            );
            return bonoGuardado;
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> getCitasPorFecha(LocalDate fecha) {
        log.info("[DEBUG-TENANT] Servicio: CitaServiceImpl | Agenda para fecha: {} | TenantContext: {}", fecha, com.example.quiropracticoapi.config.TenantContext.getTenantId());
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
    @Transactional(readOnly = true)
    public Page<CitaDto> getCitasPorCliente(Integer idCliente, EstadoCita estado, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : null;

        Page<Cita> page = citaRepository.findByClienteAndFiltros(idCliente, estado, inicio, fin, pageable);

        return page.map(c -> {
            CitaDto dto = citaMapper.toDto(c);
            try {
                llenarInfoPago(c, dto);
            } catch (Exception e) {
                // log error pero no romper
            }
            return dto;
        });
    }

    @Override
    @Transactional
    public void cancelarCita(Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        EstadoCita estadoAnterior = cita.getEstado();
        cita.setEstado(EstadoCita.cancelada);
        citaRepository.save(cita);
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "CITA",
                idCita.toString(),
                "Cita cancelada. Estado anterior: " + estadoAnterior + ". Cliente: " + cita.getCliente().getNombre()
        );

        // Devolver sesión de bono si aplica
        var consumoOpt = consumoBonoRepository.findByCitaIdCita(idCita);
        if (consumoOpt.isPresent()) {
            BonoActivo bono = consumoOpt.get().getBonoActivo();
            bono.setSesionesRestantes(bono.getSesionesRestantes() + 1);
            bonoActivoRepository.save(bono);
            // Usamos query JPQL directa para evitar conflicto con CascadeType.ALL en Cita -> ConsumoBono
            consumoBonoRepository.deleteByCitaIdCita(idCita);
        }
    }

    @Override
    @Transactional(readOnly = true)
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
        EstadoCita estadoAnterior = cita.getEstado();
        cita.setEstado(nuevoEstado);

        // Si cambia de COMPLETADA a otra cosa, limpiar firma y justificante
        BonoActivo bonoAfectado = null;
        if (estadoAnterior == EstadoCita.completada && nuevoEstado != EstadoCita.completada) {
            limpiarFirmaYJustificante(cita);
        }

        // Si pasa a CANCELADA o AUSENTE (y antes no lo era), devolver sesión
        if ((nuevoEstado == EstadoCita.cancelada || nuevoEstado == EstadoCita.ausente) &&
            (estadoAnterior != EstadoCita.cancelada && estadoAnterior != EstadoCita.ausente)) {
            bonoAfectado = devolverSesionBonoSiAplica(cita);
        }

        // Si pasa de CANCELADA o AUSENTE a activa (PROGRAMADA o COMPLETADA), re-consumir sesión
        if ((estadoAnterior == EstadoCita.cancelada || estadoAnterior == EstadoCita.ausente) &&
            (nuevoEstado == EstadoCita.programada || nuevoEstado == EstadoCita.completada)) {
            bonoAfectado = reconsumirSesionBonoSiAplica(cita);
        }

        Cita citaGuardada = citaRepository.save(cita);

        // Si fue un cambio que afectó a la cartilla, regenerar (pasando el bono capturado)
        if (estadoAnterior == EstadoCita.completada && nuevoEstado != EstadoCita.completada) {
            actualizarJustificantesPostCambio(citaGuardada, bonoAfectado);
        }

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "CITA",
                idCita.toString(),
                "Cambio de estado: " + estadoAnterior + " -> " + nuevoEstado
        );
        CitaDto dto = citaMapper.toDto(citaGuardada);
        llenarInfoPago(citaGuardada, dto);
        return dto;
    }

    private void limpiarFirmaYJustificante(Cita cita) {
        cita.setFirmada(false);
        cita.setFirmaBase64(null);
        cita.setRutaJustificante(null);
    }

    private BonoActivo devolverSesionBonoSiAplica(Cita cita) {
        var consumoOpt = consumoBonoRepository.findByCitaIdCita(cita.getIdCita());
        if (consumoOpt.isPresent()) {
            ConsumoBono consumo = consumoOpt.get();
            BonoActivo bono = consumo.getBonoActivo();
            
            // Guardar memoria del bono pre-asignado
            cita.setIdBonoPreasignado(bono.getIdBonoActivo());
            
            bono.setSesionesRestantes(bono.getSesionesRestantes() + 1);
            BonoActivo bonoGuardado = bonoActivoRepository.save(bono);
            consumoBonoRepository.deleteByCitaIdCita(cita.getIdCita());
            cita.setConsumoBono(null);
            
            auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "BONO",
                bono.getIdBonoActivo().toString(),
                "Sesión devuelta (revertido) por cambio de estado en Cita ID: " + cita.getIdCita()
            );
            return bonoGuardado;
        }
        return null;
    }

    private BonoActivo reconsumirSesionBonoSiAplica(Cita cita) {
        if (cita.getIdBonoPreasignado() != null && consumoBonoRepository.findByCitaIdCita(cita.getIdCita()).isEmpty()) {
            BonoActivo bono = bonoActivoRepository.findById(cita.getIdBonoPreasignado()).orElse(null);
            if (bono != null && bono.getSesionesRestantes() > 0) {
                int saldoAnterior = bono.getSesionesRestantes();
                int nuevoSaldo = saldoAnterior - 1;
                bono.setSesionesRestantes(nuevoSaldo);
                BonoActivo bonoGuardado = bonoActivoRepository.save(bono);

                ConsumoBono consumo = new ConsumoBono();
                consumo.setCita(cita);
                consumo.setBonoActivo(bonoGuardado);
                consumo.setSesionesRestantesSnapshot(nuevoSaldo);
                if (cita.getClinica() != null) {
                    consumo.setClinica(cita.getClinica());
                }
                consumoBonoRepository.save(consumo);

                cita.setIdBonoPreasignado(null);
                
                auditoriaServiceImpl.registrarAccion(
                    TipoAccion.CONSUMO,
                    "BONO",
                    bonoGuardado.getIdBonoActivo().toString(),
                    "Sesión re-consumida por cambio de estado en Cita ID: " + cita.getIdCita() +
                        ". Saldo: " + saldoAnterior + " -> " + nuevoSaldo
                );
                return bonoGuardado;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public CitaDto updateCita(Integer idCita, CitaRequestDto request) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        
        BonoActivo bonoAfectado = null;

        Usuario quiro = usuarioRepository.findById(request.getIdQuiropractico())
                .orElseThrow(() -> new ResourceNotFoundException("Quiropráctico no encontrado"));
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        validarDisponibilidad(quiro, request, idCita);

        EstadoCita estadoAnterior = cita.getEstado(); // Capturar antes de modificar
        String cambios = "";
        if (!cita.getQuiropractico().getIdUsuario().equals(quiro.getIdUsuario())) cambios += "Quiropráctico cambiado. ";
        if (!cita.getFechaHoraInicio().isEqual(request.getFechaHoraInicio())) cambios += "Fecha reprogramada. ";

        cita.setQuiropractico(quiro);
        cita.setCliente(cliente);
        cita.setFechaHoraInicio(request.getFechaHoraInicio());
        cita.setFechaHoraFin(request.getFechaHoraFin());
        cita.setNotasRecepcion(request.getNotasRecepcion());

        if (request.getEstado() != null) {
            try {
                EstadoCita nuevoEst = EstadoCita.valueOf(request.getEstado().toLowerCase());
                
                // Si cambia de COMPLETADA a otra cosa, limpiar firma y justificante
                if (estadoAnterior == EstadoCita.completada && nuevoEst != EstadoCita.completada) {
                    limpiarFirmaYJustificante(cita);
                }

                // Si pasa a CANCELADA o AUSENTE (y antes no lo era), devolver sesión
                if ((nuevoEst == EstadoCita.cancelada || nuevoEst == EstadoCita.ausente) &&
                    (estadoAnterior != EstadoCita.cancelada && estadoAnterior != EstadoCita.ausente)) {
                    bonoAfectado = devolverSesionBonoSiAplica(cita);
                }

                // Si pasa de CANCELADA o AUSENTE a activa (PROGRAMADA o COMPLETADA), re-consumir sesión
                if ((estadoAnterior == EstadoCita.cancelada || estadoAnterior == EstadoCita.ausente) &&
                    (nuevoEst == EstadoCita.programada || nuevoEst == EstadoCita.completada)) {
                    bonoAfectado = reconsumirSesionBonoSiAplica(cita);
                }

                cita.setEstado(nuevoEst);
            } catch (IllegalArgumentException e) {
                log.error("Error al establecer nuevo estado: {}", e.getMessage());
            }
        }

        Cita actualizada = citaRepository.save(cita);

        // Si fue una reversión de COMPLETADA, regenerar cartilla (pasando el bono capturado)
        if (estadoAnterior == EstadoCita.completada && actualizada.getEstado() != EstadoCita.completada) {
            actualizarJustificantesPostCambio(actualizada, bonoAfectado);
        }

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "CITA",
                idCita.toString(),
                "Edición de datos. " + cambios + "Nuevas notas: " + request.getNotasRecepcion()
        );
        CitaDto dto = citaMapper.toDto(actualizada);
        llenarInfoPago(actualizada, dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional(readOnly = true)
    public Page<CitaDto> getAllCitas(String search, EstadoCita estado, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : null;

        Page<Cita> page = citaRepository.findAllWithFilters(search, estado, inicio, fin, pageable);

        return page.map(c -> {
            CitaDto dto = citaMapper.toDto(c);
            try {
                llenarInfoPago(c, dto);
            } catch (Exception e) {
                // log error pero no romper
            }
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public com.example.quiropracticoapi.dto.CitasKpiDto getCitasKpis(String search, EstadoCita estado, LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : null;

        long total = citaRepository.countAllCitasInFilters(search, estado, inicio, fin);
        long programadas = citaRepository.countCitasByEstadoInFilters(search, estado, EstadoCita.programada, inicio, fin);
        long completadas = citaRepository.countCitasByEstadoInFilters(search, estado, EstadoCita.completada, inicio, fin);
        long canceladas = citaRepository.countCitasByEstadoInFilters(search, estado, EstadoCita.cancelada, inicio, fin);
        long ausentes = citaRepository.countCitasByEstadoInFilters(search, estado, EstadoCita.ausente, inicio, fin);

        return new com.example.quiropracticoapi.dto.CitasKpiDto(total, programadas, completadas, canceladas, ausentes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> getCitasPorRango(LocalDate desde, LocalDate hasta, Integer idQuiropractico) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        List<Cita> citas;
        if (idQuiropractico != null) {
            citas = citaRepository.findByQuiropracticoIdUsuarioAndFechaHoraInicioBetween(idQuiropractico, inicio, fin);
        } else {
            citas = citaRepository.findByFechaHoraInicioBetween(inicio, fin);
        }

        return citas.stream().map(c -> {
            CitaDto dto = citaMapper.toDto(c);
            llenarInfoPago(c, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    private void llenarInfoPago(Cita cita, CitaDto dto) {
        if (cita.getConsumoBono() != null) {
            ConsumoBono consumo = cita.getConsumoBono();
            BonoActivo bono = consumo.getBonoActivo();
            String nombreBono = bono.getServicioComprado().getNombreServicio();

            int restantesHistorico = consumo.getSesionesRestantesSnapshot() != null
                    ? consumo.getSesionesRestantesSnapshot()
                    : bono.getSesionesRestantes();
            String infoSaldo = " / quedan " + restantesHistorico;

            if (bono.getCliente().getIdCliente().equals(cita.getCliente().getIdCliente())) {
                dto.setInfoPago("Bono Propio (" + nombreBono  + infoSaldo+")");
                // Es el propietario del bono
                dto.setIdBonoCliente(bono.getCliente().getIdCliente());
            } else {
                dto.setInfoPago("Bono de " + bono.getCliente().getNombre() + " (" + nombreBono + infoSaldo + ")");
                // Es pagado por un familiar
                dto.setIdBonoCliente(bono.getCliente().getIdCliente());
            }
        } else if (cita.getIdBonoPreasignado() != null) {
            // No podemos consultar el repositorio en un bucle, así que indicamos que está preasignado.
            dto.setInfoPago("Bono Preasignado (Pendiente)");
        } else {
            dto.setInfoPago("Pago Directo / Sesión Suelta");
        }
    }
    @Override
    public CitaDto procesarFirma(Integer idCita, String base64Firma) {
        // PASO 1 (Transacción BD corta): Guardar rúbrica y marcar estado COMPLETADA
        Cita citaGuardada = transactionTemplate.execute(status -> {
            Cita cita = citaRepository.findById(idCita)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

            cita.setFirmaBase64(base64Firma);
            cita.setEstado(EstadoCita.completada);
            cita.setFirmada(true);

            Cita guardada = citaRepository.save(cita);

            // Re-consumir bono si aplica
            reconsumirSesionBonoSiAplica(guardada);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.EDITAR,
                    "CITA",
                    idCita.toString(),
                    "Firma procesada. Cita marcada como COMPLETADA."
            );

            return guardada;
        });

        // PASO 2: Generar PDF y subir a Cloudflare R2 (I/O de red FUERA de transacción)
        byte[] pdfBytes = ejecutarGeneracionYSubidaJustificante(citaGuardada, null);

        // PASO 3: Construcción de DTO final
        Cita citaFinal = citaRepository.findById(idCita).orElse(citaGuardada);
        CitaDto dto = citaMapper.toDto(citaFinal);
        llenarInfoPago(citaFinal, dto);

        if (pdfBytes != null) {
            dto.setSignedPdfBase64(java.util.Base64.getEncoder().encodeToString(pdfBytes));
        }

        // Notificar al WebSocket para que el frontend (CitasView/Dialog) se actualice solo
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("action", "CITA_FIRMADA");
        payload.put("idCita", idCita);
        messagingTemplate.convertAndSend("/topic/citas", payload);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarBorradorPdf(Integer idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        java.util.List<PdfFirmaService.SesionInfo> sesiones = new ArrayList<>();
        BonoActivo bono = null;

        var consumoOpt = consumoBonoRepository.findByCitaIdCita(idCita);
        
        if (consumoOpt.isPresent()) {
            bono = consumoOpt.get().getBonoActivo();
            List<ConsumoBono> consumosDelBono = consumoBonoRepository.findByBonoActivoIdBonoActivoOrderByFechaCreacionAsc(bono.getIdBonoActivo());
            
            for (int i = 0; i < bono.getSesionesTotales(); i++) {
                if (i < consumosDelBono.size()) {
                    ConsumoBono cb = consumosDelBono.get(i);
                    Cita c = cb.getCita();
                    String firmaLimpia = (c.getFirmaBase64() != null) ? PdfFirmaService.limpiarBase64(c.getFirmaBase64()) : null;
                    
                    String rangoHora = c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                       c.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));

                    sesiones.add(new PdfFirmaService.SesionInfo(
                        i + 1,
                        c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        rangoHora,
                        firmaLimpia
                    ));
                } else {
                    sesiones.add(new PdfFirmaService.SesionInfo(i + 1, null, null, null));
                }
            }
        } else if (cita.getIdBonoPreasignado() != null) {
            // Cita revertida: no tiene consumo pero sí bono pre-asignado
            bono = bonoActivoRepository.findById(cita.getIdBonoPreasignado()).orElse(null);
            
            if (bono != null) {
                List<ConsumoBono> consumosDelBono = consumoBonoRepository.findByBonoActivoIdBonoActivoOrderByFechaCreacionAsc(bono.getIdBonoActivo());
                
                // Añadir las sesiones ya consumidas del bono
                int idx = 0;
                for (; idx < consumosDelBono.size(); idx++) {
                    ConsumoBono cb = consumosDelBono.get(idx);
                    Cita c = cb.getCita();
                    String firmaLimpia = (c.getFirmaBase64() != null) ? PdfFirmaService.limpiarBase64(c.getFirmaBase64()) : null;
                    
                    String rangoHora = c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                       c.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));

                    sesiones.add(new PdfFirmaService.SesionInfo(
                        idx + 1,
                        c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        rangoHora,
                        firmaLimpia
                    ));
                }
                
                // Añadir la cita actual como siguiente sesión (borrador sin firma)
                String rangoHoraActual = cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                         cita.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
                sesiones.add(new PdfFirmaService.SesionInfo(
                    idx + 1,
                    cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rangoHoraActual,
                    null
                ));
                idx++;
                
                // Rellenar sesiones vacías restantes
                for (; idx < bono.getSesionesTotales(); idx++) {
                    sesiones.add(new PdfFirmaService.SesionInfo(idx + 1, null, null, null));
                }
            } else {
                // Bono pre-asignado ya no existe, tratar como sesión suelta
                String rangoHora = cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                   cita.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
                sesiones.add(new PdfFirmaService.SesionInfo(
                    1,
                    cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    rangoHora,
                    null
                ));
            }
        } else {
            String rangoHora = cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                               cita.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
            
            sesiones.add(new PdfFirmaService.SesionInfo(
                1,
                cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                rangoHora,
                null // Borrador sin firma
            ));
        }

        return pdfFirmaService.generarPdfBono(
            cita.getCliente().getNombre() + " " + cita.getCliente().getApellidos(),
            cita.getCliente().getIdCliente().toString(),
            cita.getQuiropractico().getNombreCompleto(),
            (bono != null && bono.getServicioComprado() != null) ? bono.getServicioComprado().getNombreServicio() : "Sesión Suelta",
            bono != null ? "#B-" + bono.getIdBonoActivo() : "S-Digital",
            bono != null && bono.getFechaCaducidad() != null ? bono.getFechaCaducidad().toString() : "Sin caducidad",
            sesiones
        );
    }

    private void registrarDocumentoEnFicha(Cita cita, String path, String nombre, Long tamanyo) {
        var docExistente = documentoClienteRepository.findByPathArchivo(path);
        
        DocumentoCliente doc;
        if (docExistente.isPresent()) {
            doc = docExistente.get();
        } else {
            doc = new DocumentoCliente();
            doc.setCliente(cita.getCliente());
            
            Clinica clinica = cita.getClinica();
            if (clinica == null && cita.getCliente() != null) {
                clinica = cita.getCliente().getClinica();
            }
            doc.setClinica(clinica);
            doc.setPathArchivo(path);
            doc.setTipoDocumento(com.example.quiropracticoapi.model.enums.TipoDocumento.JUSTIFICANTE_ASISTENCIA);
            doc.setMimeType("application/pdf");
            doc.setFechaCreacion(LocalDateTime.now());
        }
        
        // Asegurar campos no nulos ante posibles registros previos incompletos
        if (doc.getClinica() == null) {
            Clinica clinica = cita.getClinica();
            if (clinica == null && cita.getCliente() != null) {
                clinica = cita.getCliente().getClinica();
            }
            doc.setClinica(clinica);
        }
        if (doc.getMimeType() == null) {
            doc.setMimeType("application/pdf");
        }
        if (doc.getTipoDocumento() == null) {
            doc.setTipoDocumento(com.example.quiropracticoapi.model.enums.TipoDocumento.JUSTIFICANTE_ASISTENCIA);
        }
        
        doc.setNombreOriginal(nombre.endsWith(".pdf") ? nombre : nombre + ".pdf");
        doc.setTamanyoBytes(tamanyo != null ? tamanyo : 0L);
        doc.setEstadoSubida(com.example.quiropracticoapi.model.enums.EstadoSubida.ACTIVO);
        doc.setCita(cita);
        doc.setActivo(true);
        if (doc.getFechaCreacion() == null) {
            doc.setFechaCreacion(LocalDateTime.now());
        }
        
        documentoClienteRepository.save(doc);
    }

    private static class PdfGenerationData {
        private final boolean generar;
        private final String pathR2;
        private final String nombreDocumento;
        private final String nombreCliente;
        private final String idCliente;
        private final String nombreProfesional;
        private final String nombreServicio;
        private final String idBono;
        private final String fechaCaducidad;
        private final List<PdfFirmaService.SesionInfo> sesiones;

        public PdfGenerationData(boolean generar, String pathR2, String nombreDocumento,
                                 String nombreCliente, String idCliente, String nombreProfesional,
                                 String nombreServicio, String idBono, String fechaCaducidad,
                                 List<PdfFirmaService.SesionInfo> sesiones) {
            this.generar = generar;
            this.pathR2 = pathR2;
            this.nombreDocumento = nombreDocumento;
            this.nombreCliente = nombreCliente;
            this.idCliente = idCliente;
            this.nombreProfesional = nombreProfesional;
            this.nombreServicio = nombreServicio;
            this.idBono = idBono;
            this.fechaCaducidad = fechaCaducidad;
            this.sesiones = sesiones;
        }

        public static PdfGenerationData noGenerar() {
            return new PdfGenerationData(false, null, null, null, null, null, null, null, null, null);
        }

        public boolean isGenerar() { return generar; }
        public String getPathR2() { return pathR2; }
        public String getNombreDocumento() { return nombreDocumento; }
        public String getNombreCliente() { return nombreCliente; }
        public String getIdCliente() { return idCliente; }
        public String getNombreProfesional() { return nombreProfesional; }
        public String getNombreServicio() { return nombreServicio; }
        public String getIdBono() { return idBono; }
        public String getFechaCaducidad() { return fechaCaducidad; }
        public List<PdfFirmaService.SesionInfo> getSesiones() { return sesiones; }
    }

    private PdfGenerationData prepararDatosPdf(Cita cita, BonoActivo bonoForzado) {
        java.util.List<PdfFirmaService.SesionInfo> sesiones = new ArrayList<>();
        String pathR2;
        String nombreDocumento;
        BonoActivo bonoAUsar = bonoForzado;

        if (bonoAUsar == null) {
            var consumption = consumoBonoRepository.findByCitaIdCita(cita.getIdCita());
            if (consumption.isPresent()) {
                bonoAUsar = consumption.get().getBonoActivo();
            }
        }

        Long clinicaId = (cita.getClinica() != null && cita.getClinica().getIdClinica() != null)
                ? cita.getClinica().getIdClinica()
                : (cita.getCliente() != null && cita.getCliente().getClinica() != null)
                        ? cita.getCliente().getClinica().getIdClinica()
                        : com.example.quiropracticoapi.config.TenantContext.getTenantId();
        
        if (bonoAUsar != null) {
            List<ConsumoBono> consumosDelBono = consumoBonoRepository.findByBonoActivoIdBonoActivoOrderByFechaCreacionAsc(bonoAUsar.getIdBonoActivo());
            
            String nombreS = (bonoAUsar.getServicioComprado() != null && bonoAUsar.getServicioComprado().getNombreServicio() != null) 
                             ? bonoAUsar.getServicioComprado().getNombreServicio() 
                             : "Bono";
            String nombreServicioClean = nombreS.replaceAll("\\s+", "_");
            pathR2 = storagePathBuilder.buildRutaJustificanteBono(clinicaId, cita.getCliente().getIdCliente(), nombreServicioClean, bonoAUsar.getIdBonoActivo());
            nombreDocumento = nombreServicioClean + "_" + bonoAUsar.getIdBonoActivo();

            // Si no hay consumos (porque acabamos de cancelar la única cita), borrado puramente LÓGICO en BD (sin borrar de R2)
            if (consumosDelBono.isEmpty()) {
                documentoClienteRepository.findByPathArchivo(pathR2).ifPresent(doc -> {
                    doc.setActivo(false);
                    doc.setEstadoSubida(EstadoSubida.ELIMINADO);
                    doc.setFechaEliminacionLogica(LocalDateTime.now());
                    documentoClienteRepository.save(doc);
                });
                return PdfGenerationData.noGenerar();
            }

            for (int i = 0; i < bonoAUsar.getSesionesTotales(); i++) {
                if (i < consumosDelBono.size()) {
                    ConsumoBono cb = consumosDelBono.get(i);
                    Cita c = cb.getCita();
                    String firmaLimpia = (c.getFirmaBase64() != null) ? PdfFirmaService.limpiarBase64(c.getFirmaBase64()) : null;
                    
                    String rangoHora = c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                                       c.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));

                    sesiones.add(new PdfFirmaService.SesionInfo(
                        i + 1,
                        c.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        rangoHora,
                        firmaLimpia
                    ));
                } else {
                    sesiones.add(new PdfFirmaService.SesionInfo(i + 1, null, null, null));
                }
            }
        } else {
            // Caso de Sesión Suelta: si no está completada, borrado puramente LÓGICO en BD (sin borrar de R2)
            if (cita.getEstado() != EstadoCita.completada) {
                pathR2 = storagePathBuilder.buildRutaJustificante(clinicaId, cita.getCliente().getIdCliente(), cita.getIdCita());
                documentoClienteRepository.findByPathArchivo(pathR2).ifPresent(doc -> {
                    doc.setActivo(false);
                    doc.setEstadoSubida(EstadoSubida.ELIMINADO);
                    doc.setFechaEliminacionLogica(LocalDateTime.now());
                    documentoClienteRepository.save(doc);
                });
                return PdfGenerationData.noGenerar();
            }

            String rangoHora = cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                               cita.getFechaHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
            
            sesiones.add(new PdfFirmaService.SesionInfo(
                1,
                cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                rangoHora,
                (cita.getFirmaBase64() != null) ? PdfFirmaService.limpiarBase64(cita.getFirmaBase64()) : null
            ));
            pathR2 = storagePathBuilder.buildRutaJustificante(clinicaId, cita.getCliente().getIdCliente(), cita.getIdCita());
            nombreDocumento = "Sesion_Suelta_" + cita.getIdCita();
        }

        return new PdfGenerationData(
            true,
            pathR2,
            nombreDocumento,
            cita.getCliente().getNombre() + " " + cita.getCliente().getApellidos(),
            cita.getCliente().getIdCliente().toString(),
            cita.getQuiropractico().getNombreCompleto(),
            (bonoAUsar != null && bonoAUsar.getServicioComprado() != null) ? bonoAUsar.getServicioComprado().getNombreServicio() : "Sesión Suelta",
            bonoAUsar != null ? "#B-" + bonoAUsar.getIdBonoActivo() : "S-Digital",
            bonoAUsar != null && bonoAUsar.getFechaCaducidad() != null ? bonoAUsar.getFechaCaducidad().toString() : "Sin caducidad",
            sesiones
        );
    }

    private byte[] ejecutarGeneracionYSubidaJustificante(Cita cita, BonoActivo bonoForzado) {
        // PASO A: Preparar datos dentro de transacción corta de lectura (evitando LazyInitializationException)
        PdfGenerationData data = transactionTemplate.execute(status -> prepararDatosPdf(cita, bonoForzado));

        if (data == null || !data.isGenerar()) {
            return null;
        }

        // PASO B: Generar PDF en memoria RAM (CPU bound, sin BD)
        byte[] pdfBytes = pdfFirmaService.generarPdfBono(
            data.getNombreCliente(),
            data.getIdCliente(),
            data.getNombreProfesional(),
            data.getNombreServicio(),
            data.getIdBono(),
            data.getFechaCaducidad(),
            data.getSesiones()
        );

        // PASO C (I/O DE RED FUERA DE TRANSACCIÓN): Subir a Cloudflare R2 sin retener conexión de BD
        r2StorageService.storeBytes(pdfBytes, data.getPathR2(), "application/pdf");

        // PASO D: Post-Update en BD dentro de micro-transacción
        transactionTemplate.executeWithoutResult(status -> {
            if (cita.getEstado() == EstadoCita.completada) {
                Cita c = citaRepository.findById(cita.getIdCita()).orElse(cita);
                c.setRutaJustificante(data.getPathR2());
                citaRepository.save(c);
            }
            registrarDocumentoEnFicha(cita, data.getPathR2(), data.getNombreDocumento(), (long) pdfBytes.length);
        });

        return pdfBytes;
    }

    private byte[] actualizarJustificantesPostCambio(Cita cita, BonoActivo bonoForzado) {
        return ejecutarGeneracionYSubidaJustificante(cita, bonoForzado);
    }
}
