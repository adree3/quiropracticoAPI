package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.mapper.ClienteMapper;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }


    @Override
    public List<ClienteDto> getAllClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toClienteDto)
                .collect(Collectors.toList());
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
        if (!clienteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + id);
        }
        clienteRepository.deleteById(id);
    }

    @Override
    public List<ClienteDto> searchClientesByApellidos(String textoApellidos) {
        return clienteRepository.findByApellidosContainingIgnoreCase(textoApellidos)
                .stream()
                .map(clienteMapper::toClienteDto)
                .collect(Collectors.toList());
    }
}
