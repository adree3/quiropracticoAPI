package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.ClienteMapper;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.GrupoFamiliar;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.GrupoFamiliarRepository;
import com.example.quiropracticoapi.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final GrupoFamiliarRepository grupoFamiliarRepository;


    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository, ClienteMapper clienteMapper, GrupoFamiliarRepository grupoFamiliarRepository) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
    }


    @Override
    public Page<ClienteDto> getAllClientes(Boolean activo,Pageable pageable) {
        return clienteRepository.findByActivo(activo, pageable).map(clienteMapper::toClienteDto);
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
        // Si quiero que el telefeono sea unico pongo otra excepcion aqui
        Cliente cliente = clienteMapper.toCliente(clienteRequestDto);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        return clienteMapper.toClienteDto(clienteGuardado);
    }

    @Override
    public ClienteDto updateCliente(Integer id, ClienteRequestDto clienteRequestDto) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        clienteMapper.updateClienteFromDto(clienteRequestDto, clienteExistente);

        Cliente clienteActualizado = clienteRepository.save(clienteExistente);

        return clienteMapper.toClienteDto(clienteActualizado);
    }

    @Override
    public void deleteCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);
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

        grupoFamiliarRepository.save(grupo);
    }

    @Override
    public void recoverCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.setActivo(true);
        clienteRepository.save(cliente);
    }

    private Cliente getClienteByIdEntity(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }
}
