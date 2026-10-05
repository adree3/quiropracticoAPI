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
    private final com.example.quiropracticoapi.repository.PagoRepository pagoRepository;
    private final com.example.quiropracticoapi.mapper.CitaMapper citaMapper;



    @Autowired
    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            ClienteMapper clienteMapper,
            GrupoFamiliarRepository grupoFamiliarRepository,
            CitaRepository citaRepository,
            BonoActivoRepository bonoActivoRepository, 
            AuditoriaServiceImpl auditoriaServiceImpl, 
            BonoServiceImpl bonoServiceImpl,
            com.example.quiropracticoapi.repository.PagoRepository pagoRepository,
            com.example.quiropracticoapi.mapper.CitaMapper citaMapper
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.citaRepository = citaRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
        this.bonoServiceImpl = bonoServiceImpl;
        this.pagoRepository = pagoRepository;
        this.citaMapper = citaMapper;
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
        return enrichClienteDetalleDto(cliente);
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
    public Page<ClienteDto> findClientesWithFilters(
        Boolean activo,
        String texto,
        Integer lastActivityDays,
        Pageable pageable
    ) {
        // Para filtros temporales complejos (lastActivityDays) seguimos usando el filtrado por entidad
        LocalDateTime ultimaCitaDesde = null;
        if (lastActivityDays != null) {
            ultimaCitaDesde = LocalDateTime.now().minusDays(lastActivityDays);
        }

        Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();

        log.info("[DEBUG-TENANT] Servicio: ClienteServiceImpl | TenantContext: {} | Texto: {}", tenantId, texto);
        // Búsqueda normal optimizada (Sin N+1)
        Page<ClienteDetalleProjection> projectionPage = clienteRepository.findClientesOptimized(
            activo, texto, tenantId, ultimaCitaDesde, pageable
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

    private ClienteDto enrichClienteDetalleDto(Cliente cliente) {
        ClienteDto dto = clienteMapper.toClienteDto(cliente);
        Integer idCliente = cliente.getIdCliente();
        
        // Citas métricas
        long completadas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.completada);
        long canceladas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.cancelada);
        long ausentes = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.ausente);
        long programadas = citaRepository.countByClienteIdClienteAndEstado(idCliente, EstadoCita.programada);
        
        dto.setCitasPendientes((int) programadas);
        dto.setCitasCompletadas(completadas);
        dto.setCitasCanceladas(canceladas);
        dto.setCitasAusentes(ausentes);
        
        // Pagos métricas
        dto.setDeudaPendiente(pagoRepository.sumDeudaPendienteByClienteId(idCliente));
        dto.setPagosPendientesCount(pagoRepository.countByClienteIdClienteAndPagadoFalse(idCliente));
        
        // Bonos activos (Conteo escalar)
        long bonosActivosCount = bonoActivoRepository.countByClienteIdClienteAndSesionesRestantesGreaterThan(idCliente, 0);
        dto.setBonosActivos((int) bonosActivosCount);
        
        // Familiares (Exists escalar)
        boolean tieneFamiliares = grupoFamiliarRepository.existsByPropietarioIdCliente(idCliente);
        dto.setTieneFamiliares(tieneFamiliares);
        
        // Última cita (Scalar Max Date)
        LocalDateTime ultimaCita = citaRepository.findUltimaCitaCompletadaFecha(idCliente);
        dto.setUltimaCita(ultimaCita);
        
        citaRepository.findFirstByClienteIdClienteAndEstadoAndFechaHoraInicioGreaterThanEqualOrderByFechaHoraInicioAsc(
            idCliente, EstadoCita.programada, LocalDateTime.now()
        ).ifPresent(cita -> {
            dto.setProximaCita(citaMapper.toDto(cita));
        });
        
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
