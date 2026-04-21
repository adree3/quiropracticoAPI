package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BonoHistoricoDto;
import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.dto.ConsumoBonoDto;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.ConsumoBono;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.repository.ConsumoBonoRepository;
import com.example.quiropracticoapi.service.BonoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BonoServiceImpl implements BonoService {

    private final BonoActivoRepository bonoActivoRepository;
    private final ConsumoBonoRepository consumoBonoRepository;
    private final AuditoriaServiceImpl auditoriaServiceImpl;

    @Autowired
    public BonoServiceImpl(BonoActivoRepository bonoActivoRepository,
                           ConsumoBonoRepository consumoBonoRepository,
                           AuditoriaServiceImpl auditoriaServiceImpl) {
        this.bonoActivoRepository = bonoActivoRepository;
        this.consumoBonoRepository = consumoBonoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    @Override
    public List<BonoSeleccionDto> getBonosUsables(Integer idCliente) {
        return bonoActivoRepository.findBonosDisponiblesParaCliente(idCliente).stream()
                .map(b -> {
                    BonoSeleccionDto dto = new BonoSeleccionDto();
                    dto.setIdBonoActivo(b.getIdBonoActivo());
                    dto.setNombreServicio(b.getServicioComprado().getNombreServicio());
                    dto.setSesionesRestantes(b.getSesionesRestantes());

                    // Determinar nombre a mostrar
                    if (b.getCliente().getIdCliente().equals(idCliente)) {
                        dto.setPropietarioNombre(b.getCliente().getNombre() + " (Propio)");
                        dto.setEsPropio(true);
                    } else {
                        dto.setPropietarioNombre(b.getCliente().getNombre() + " (Familiar)");
                        dto.setEsPropio(false);
                    }

                    if (b.getPagoOrigen() != null) {
                        dto.setEsPagado(b.getPagoOrigen().isPagado());
                    } else {
                        dto.setEsPagado(false); 
                    }

                    dto.setFechaCompra(b.getFechaCompra());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void devolverSesion(Integer idBonoActivo) {
        devolverSesion(idBonoActivo, null);
    }

    @Override
    @Transactional
    public void devolverSesion(Integer idBonoActivo, Integer idCita) {
        BonoActivo bono = bonoActivoRepository.findById(idBonoActivo)
                .orElseThrow(() -> new RuntimeException("Error crítico: Se intenta devolver sesión a un bono inexistente ID: " + idBonoActivo));
        int saldoAnterior = bono.getSesionesRestantes();
        bono.setSesionesRestantes(bono.getSesionesRestantes() + 1);

        bonoActivoRepository.save(bono);

        // Si hay Cita, eliminar el registro de consumo asociado para limpiar el historial
        if (idCita != null) {
            // Buscamos el consumo asociado a esta cita
            var consumoOpt = consumoBonoRepository.findByCitaIdCita(idCita);
            consumoOpt.ifPresent(consumoBonoRepository::delete);
        }

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "BONO",
                bono.getIdBonoActivo().toString(),
                "Sesión devuelta al bono (normalmente por cancelación de cita). " +
                        "Cliente: " + bono.getCliente().getNombre() + " " + bono.getCliente().getApellidos() +
                        ". Saldo actualizado: " + saldoAnterior + " -> " + bono.getSesionesRestantes()
        );
    }

    @Override
    @Transactional
    public void consumirSesion(Integer idBonoActivo) {
        consumirSesion(idBonoActivo, null);
    }

    @Override
    @Transactional
    public void consumirSesion(Integer idBonoActivo, Integer idCita) {
        BonoActivo bono = bonoActivoRepository.findById(idBonoActivo)
                .orElseThrow(() -> new RuntimeException("Error crítico: Se intenta consumir sesión de un bono inexistente ID: " + idBonoActivo));

        if (bono.getSesionesRestantes() <= 0) {
            throw new RuntimeException("No quedan sesiones en el bono para restaurar la cita.");
        }

        int saldoAnterior = bono.getSesionesRestantes();
        bono.setSesionesRestantes(saldoAnterior - 1);
        BonoActivo guardado = bonoActivoRepository.save(bono);

        // Si hay cita, crear registro de ConsumoBono
        if (idCita != null) {
            ConsumoBono consumo = new ConsumoBono();
            Cita citaRef = new Cita();
            citaRef.setIdCita(idCita); 
            consumo.setCita(citaRef);
            consumo.setBonoActivo(guardado);
            consumo.setSesionesRestantesSnapshot(guardado.getSesionesRestantes());
            
            // Asignar fecha actual como fecha de consumo
            consumo.setFechaCreacion(java.time.LocalDateTime.now());
            
            consumoBonoRepository.save(consumo);
        }

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CONSUMO,
                "BONO",
                bono.getIdBonoActivo().toString(),
                "Sesión consumida (Restauración/Undo). " +
                        "Cliente: " + bono.getCliente().getNombre() + " " + bono.getCliente().getApellidos() +
                        ". Saldo actualizado: " + saldoAnterior + " -> " + bono.getSesionesRestantes()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsumoBonoDto> getHistorialBono(Integer idBonoActivo) {
        return consumoBonoRepository.findByBonoActivoIdBonoActivo(idBonoActivo).stream()
                .map(consumo -> {
                    ConsumoBonoDto dto = new ConsumoBonoDto();
                    dto.setIdConsumo(consumo.getIdConsumo());
                    dto.setFechaConsumo(consumo.getFechaCreacion());
                    dto.setSesionesRestantesSnapshot(consumo.getSesionesRestantesSnapshot());

                    if (consumo.getCita() != null) {
                        dto.setIdCita(consumo.getCita().getIdCita());
                        dto.setFechaCita(consumo.getCita().getFechaHoraInicio());
                        if (consumo.getCita().getEstado() != null) {
                            dto.setEstadoCita(consumo.getCita().getEstado().name());
                        }
                        // Obtener nombre quiropráctico de la cita si existe
                        if (consumo.getCita().getQuiropractico() != null) {
                            dto.setNombreQuiropractico(consumo.getCita().getQuiropractico().getNombreCompleto());
                        }
                        // Obtener nombre del paciente de la cita
                        if (consumo.getCita().getCliente() != null) {
                            dto.setIdPaciente(consumo.getCita().getCliente().getIdCliente());
                            dto.setNombrePaciente(consumo.getCita().getCliente().getNombre());
                        }
                    }
                    return dto;
                })
                .sorted((a, b) -> {
                    if (b.getFechaConsumo() == null || a.getFechaConsumo() == null) return 0;
                    return b.getFechaConsumo().compareTo(a.getFechaConsumo());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BonoHistoricoDto> getHistorialBonos(String search, Pageable pageable) {
        return bonoActivoRepository.findAllWithFilters(search, pageable).map(this::toHistoricoDto);
    }

    private BonoHistoricoDto toHistoricoDto(BonoActivo b) {
        String nombreCompleto = b.getCliente().getNombre() + " " + b.getCliente().getApellidos();
        BonoHistoricoDto dto = new BonoHistoricoDto();
        dto.setIdBonoActivo(b.getIdBonoActivo());
        dto.setIdCliente(b.getCliente().getIdCliente());
        dto.setNombreCliente(nombreCompleto);
        dto.setNombreServicio(b.getServicioComprado().getNombreServicio());
        dto.setSesionesTotales(b.getSesionesTotales());
        dto.setSesionesRestantes(b.getSesionesRestantes());
        dto.setFechaCompra(b.getFechaCompra());
        dto.setFechaCaducidad(b.getFechaCaducidad());
        dto.setPagado(b.getPagoOrigen() != null && b.getPagoOrigen().isPagado());
        return dto;
    }
}
