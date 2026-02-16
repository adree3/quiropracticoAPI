package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.ClienteMapper;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.GrupoFamiliar;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.GrupoFamiliarRepository;
import com.example.quiropracticoapi.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final GrupoFamiliarRepository grupoFamiliarRepository;
    private final CitaRepository citaRepository;
    private final BonoActivoRepository bonoActivoRepository;
    private final AuditoriaServiceImpl auditoriaServiceImpl;
    private final BonoServiceImpl bonoServiceImpl;



    @Autowired
    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            ClienteMapper clienteMapper,
            GrupoFamiliarRepository grupoFamiliarRepository,
            CitaRepository citaRepository,
            BonoActivoRepository bonoActivoRepository, AuditoriaServiceImpl auditoriaServiceImpl, BonoServiceImpl bonoServiceImpl
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.citaRepository = citaRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
        this.bonoServiceImpl = bonoServiceImpl;
    }


    @Override
    public Page<ClienteDto> getAllClientes(Boolean activo, Pageable pageable) {
        Page<Cliente> paginaClientes;
        if (activo == null) {
            paginaClientes = clienteRepository.findAll(pageable);
        } else {
            paginaClientes = clienteRepository.findByActivo(activo, pageable);
        }
        return paginaClientes.map(clienteMapper::toClienteDto);
    }

    @Override
    public ClienteDto getClienteById(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el cliente con id: " + id));
        return clienteMapper.toClienteDto(cliente);
    }

    @Override
    public ClienteDto createCliente(ClienteRequestDto clienteRequestDto) {
        clienteRepository.findByEmail(clienteRequestDto.getEmail()).ifPresent(c -> {
            throw new IllegalArgumentException("El email" + clienteRequestDto.getEmail() + " ya existe.");
        });
        Cliente cliente = clienteMapper.toCliente(clienteRequestDto);
        cliente.setActivo(true);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "CLIENTE",
                clienteGuardado.getIdCliente().toString(),
                "Nuevo paciente registrado: " + clienteGuardado.getNombre() + " " + clienteGuardado.getApellidos() +
                        "."
        );
        return clienteMapper.toClienteDto(clienteGuardado);
    }

    @Override
    public ClienteDto updateCliente(Integer id, ClienteRequestDto clienteRequestDto, boolean undo) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        String datosPrevios = clienteExistente.getNombre() + " " + clienteExistente.getApellidos();
        clienteMapper.updateClienteFromDto(clienteRequestDto, clienteExistente);
        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        
        TipoAccion tipoAccion = undo ? TipoAccion.DESHACER : TipoAccion.EDITAR;
        String detalle = undo 
            ? "Deshacer edición. Restaurado a: " + clienteActualizado.getNombre() + " " + clienteActualizado.getApellidos()
            : "Actualización de datos personales. Nombre: " + clienteActualizado.getNombre() + " " + clienteActualizado.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                tipoAccion,
                "CLIENTE",
                id.toString(),
                detalle
        );

        return clienteMapper.toClienteDto(clienteActualizado);
    }

    @Override
    public void deleteCliente(Integer id, boolean undo) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        TipoAccion tipoAccion = undo ? TipoAccion.DESHACER : TipoAccion.ELIMINAR_LOGICO;
        String detalle = undo
                ? "Deshacer reactivación. Cliente desactivado nuevamente: " + cliente.getNombre() + " " + cliente.getApellidos()
                : "Paciente desactivado (enviado a papelera): " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                tipoAccion,
                "CLIENTE",
                id.toString(),
                detalle
        );
    }

    @Override
    public List<ClienteDto> searchClientesList(String texto) {
        return clienteRepository.searchGlobal(texto)
                .stream()
                .map(clienteMapper::toClienteDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ClienteDto> searchClientesPaged(String texto, Pageable pageable) {
        Page<Cliente> pagina = clienteRepository.searchGlobalPaged(texto, pageable);
        return pagina.map(clienteMapper::toClienteDto);
    }

    @Override
    public Page<ClienteDto> findClientesWithFilters(
        Boolean activo,
        String texto,
        Integer lastActivityDays,
        Pageable pageable
    ) {
        LocalDateTime ultimaCitaDesde = null;
        if (lastActivityDays != null) {
            ultimaCitaDesde = LocalDateTime.now().minusDays(lastActivityDays);
        }
        
        Page<Cliente> clientesPage = clienteRepository.findClientesFiltered(
            activo,
            texto,
            ultimaCitaDesde,
            pageable
        );
        
        // Enriquecer cada Cliente con información agregada usando repositorios
        return clientesPage.map(this::enrichClienteDto);
    }

    /**
     * Enriquece un ClienteDto con información agregada desde los repositorios
     */
    private ClienteDto enrichClienteDto(Cliente cliente) {
        ClienteDto dto = clienteMapper.toClienteDto(cliente);
        
        // Calcular citas pendientes (futuras y no canceladas/ausentes)
        LocalDateTime now = LocalDateTime.now();
        List<Cita> todasCitas = citaRepository.findByClienteIdClienteOrderByFechaHoraInicioDesc(cliente.getIdCliente());
        
        long citasPendientes = todasCitas.stream()
            .filter(c -> c.getFechaHoraInicio().isAfter(now))
            .filter(c -> c.getEstado() != EstadoCita.cancelada && c.getEstado() != EstadoCita.ausente)
            .count();
        dto.setCitasPendientes((int) citasPendientes);
        
        // Calcular bonos activos (con sesiones restantes > 0)
        // Usar el método del repositorio que ya filtra por sesiones > 0
        List<BonoActivo> bonosActivos = bonoActivoRepository.findByClienteIdClienteAndSesionesRestantesGreaterThan(
            cliente.getIdCliente(), 
            0
        );
        dto.setBonosActivos(bonosActivos.size());
        
        // Verificar si tiene familiares (es propietario de algún grupo)
        List<GrupoFamiliar> grupos = grupoFamiliarRepository.findByPropietarioIdCliente(cliente.getIdCliente());
        dto.setTieneFamiliares(!grupos.isEmpty());
        
        // Obtener última cita completada
        LocalDateTime ultimaCita = todasCitas.stream()
            .filter(c -> c.getEstado() == EstadoCita.completada)
            .map(Cita::getFechaHoraInicio)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        dto.setUltimaCita(ultimaCita);
        
        return dto;
    }

    @Override
    public void agregarFamiliar(Integer idPropietario, Integer idBeneficiario, String relacion, boolean undo, List<Integer> idsCitasARestaurar) {
        if (idPropietario.equals(idBeneficiario)) {
            throw new IllegalArgumentException("Un cliente no puede ser familiar de sí mismo.");
        }
        Cliente propietario = clienteRepository.findById(idPropietario)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con id: " + idPropietario));

        Cliente beneficiario = clienteRepository.findById(idBeneficiario)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiario no encontrado con id: " + idBeneficiario));

        if (grupoFamiliarRepository.existsByPropietarioIdClienteAndBeneficiarioIdCliente(idPropietario, idBeneficiario)) {
            throw new IllegalArgumentException("Esta relación familiar ya existe.");
        }
        GrupoFamiliar grupo = new GrupoFamiliar();
        grupo.setPropietario(propietario);
        grupo.setBeneficiario(beneficiario);
        grupo.setRelacion(relacion);

        GrupoFamiliar guardado = grupoFamiliarRepository.save(grupo);

        // Lógica de restauración de citas (Undo)
        if (undo && idsCitasARestaurar != null && !idsCitasARestaurar.isEmpty()) {
            List<Cita> citas = citaRepository.findAllById(idsCitasARestaurar);
            int restauradasCount = 0;

            for (Cita cita : citas) {
                if (cita.getEstado() == EstadoCita.cancelada) {
                   cita.setEstado(EstadoCita.programada);
                   // Limpiar o anexar nota de restauración
                   String notaRestauracion = "Cita restaurada por deshacer desvinculación.";
                   if (cita.getNotasRecepcion() != null) {
                       cita.setNotasRecepcion(cita.getNotasRecepcion() + "\n" + notaRestauracion);
                   } else {
                       cita.setNotasRecepcion(notaRestauracion);
                   }
                   
                   // Volver a consumir sesión si tenía bono asociado
                   if (cita.getConsumoBono() != null) {
                       Integer idBono = cita.getConsumoBono().getBonoActivo().getIdBonoActivo();
                        try {
                            bonoServiceImpl.consumirSesion(idBono, cita.getIdCita());
                        } catch (Exception e) {
                           // Si falla el consumo (ej. bono caducado o sin saldo), podríamos lanzar exepción o loguear
                           throw new RuntimeException("Error al restaurar cita " + cita.getIdCita() + ": " + e.getMessage());
                       }
                   }
                   restauradasCount++;
                }
            }
            citaRepository.saveAll(citas);
            if (restauradasCount > 0) {
                 auditoriaServiceImpl.registrarAccion(
                        TipoAccion.EDITAR,
                        "CITA",
                        "VARIAS (" + restauradasCount + ")",
                        "Restauración de " + restauradasCount + " citas de " + beneficiario.getNombre() +
                                " y consumo de sesiones del bono de " + propietario.getNombre()
                );
            }
        }

        TipoAccion tipoAccion = undo ? TipoAccion.DESHACER : TipoAccion.CREAR;
        String detalle = undo
                ? "Deshacer desvinculación. Relación restaurada: " + propietario.getNombre() + " -> " + beneficiario.getNombre()
                : "Relación creada: " + propietario.getNombre() + " es propietario de -> " + beneficiario.getNombre() + " (Relación: " + relacion + ")";

        auditoriaServiceImpl.registrarAccion(
                tipoAccion,
                "GRUPO_FAMILIAR",
                guardado.getIdGrupo().toString(),
                detalle
        );
    }

    @Override
    public void recoverCliente(Integer id, boolean undo) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(true);
        clienteRepository.save(cliente);
        
        TipoAccion tipoAccion = undo ? TipoAccion.DESHACER : TipoAccion.REACTIVAR;
        String detalle = undo
                ? "Deshacer eliminación. Cliente reactivado: " + cliente.getNombre() + " " + cliente.getApellidos()
                : "Paciente reactivado/restaurado: " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                tipoAccion,
                "CLIENTE",
                id.toString(),
                detalle
        );
    }

    @Override
    public void deleteFamiliar(Integer idGrupo) {
        GrupoFamiliar grupo = grupoFamiliarRepository.findById(idGrupo)
                .orElseThrow(() -> new ResourceNotFoundException("Relación familiar no encontrada"));

        grupoFamiliarRepository.deleteById(idGrupo);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.ELIMINAR_FISICO,
                "GRUPO_FAMILIAR",
                idGrupo.toString(),
                "Relación eliminada entre Propietario: " + grupo.getPropietario().getNombre() +
                        " y Beneficiario: " + grupo.getBeneficiario().getNombre()
        );
    }

    private Cliente getClienteByIdEntity(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }
}
