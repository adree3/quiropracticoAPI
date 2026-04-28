package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.dto.ClienteDetalleProjection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {
    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);
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
        return enrichClienteDto(cliente);
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
    public ClienteDto updateCliente(Integer id, ClienteRequestDto clienteRequestDto) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        String datosPrevios = clienteExistente.getNombre() + " " + clienteExistente.getApellidos();
        clienteMapper.updateClienteFromDto(clienteRequestDto, clienteExistente);
        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        
        String detalle = "Actualización de datos personales. Nombre: " + clienteActualizado.getNombre() + " " + clienteActualizado.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "CLIENTE",
                id.toString(),
                detalle
        );

        return clienteMapper.toClienteDto(clienteActualizado);
    }

    @Override
    public void deleteCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        String detalle = "Paciente desactivado (enviado a papelera): " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.ELIMINAR_LOGICO,
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
        // Para filtros temporales complejos (lastActivityDays) seguimos usando el filtrado por entidad
        // Pero para la búsqueda general (que es el 90% del uso), usamos la versión optimizada.
        if (lastActivityDays != null) {
            LocalDateTime ultimaCitaDesde = LocalDateTime.now().minusDays(lastActivityDays);
            Page<Cliente> clientesPage = clienteRepository.findClientesFiltered(
                activo, texto, ultimaCitaDesde, pageable
            );
            return clientesPage.map(this::enrichClienteDto);
        }

        Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();

        log.info("[DEBUG-TENANT] Servicio: ClienteServiceImpl | TenantContext: {} | Texto: {}", tenantId, texto);
        // Búsqueda normal optimizada (Sin N+1)
        Page<ClienteDetalleProjection> projectionPage = clienteRepository.findClientesOptimized(
            activo, texto, tenantId, pageable
        );

        return projectionPage.map(p -> {
            ClienteDto dto = new ClienteDto();
            dto.setIdCliente(p.getIdCliente());
            dto.setNombre(p.getNombre());
            dto.setApellidos(p.getApellidos());
            dto.setEmail(p.getEmail());
            dto.setTelefono(p.getTelefono());
            dto.setActivo(p.getActivo());
            dto.setFechaAlta(p.getFechaCreacion());
            
            // Datos agregados ya calculados en SQL
            dto.setCitasPendientes(p.getCountCitasPendientes());
            dto.setBonosActivos(p.getCountBonosActivos());
            dto.setTieneFamiliares(p.getTieneFamiliares() != null && p.getTieneFamiliares() > 0);
            dto.setUltimaCita(p.getUltimaCita());
            
            return dto;
        });
    }

    /**
     * Enriquece un ClienteDto con información agregada desde los repositorios
     */
    private ClienteDto enrichClienteDto(Cliente cliente) {
        ClienteDto dto = clienteMapper.toClienteDto(cliente);
        
        // Calcular citas programadas (estado = programada, tanto pasadas como futuras)
        List<Cita> todasCitas = citaRepository.findByClienteIdClienteOrderByFechaHoraInicioDesc(cliente.getIdCliente());
        
        long citasPendientes = todasCitas.stream()
            .filter(c -> c.getEstado() == EstadoCita.programada) // Todas las programadas, independientemente de la fecha
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
    public void agregarFamiliar(Integer idPropietario, Integer idBeneficiario, String relacion) {
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

        String detalle = "Relación creada: " + propietario.getNombre() + " es propietario de -> " + beneficiario.getNombre() + " (Relación: " + relacion + ")";

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "GRUPO_FAMILIAR",
                guardado.getIdGrupo().toString(),
                detalle
        );
    }

    @Override
    public void recoverCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(true);
        clienteRepository.save(cliente);
        
        String detalle = "Paciente reactivado: " + cliente.getNombre() + " " + cliente.getApellidos();

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.REACTIVAR,
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
