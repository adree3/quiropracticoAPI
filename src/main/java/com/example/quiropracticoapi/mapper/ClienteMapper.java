package com.example.quiropracticoapi.mapper;

import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    /**
     * Convierte la entidad cliente a un ClienteDTO (para respuestas)
     */
    public ClienteDto toClienteDto(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        ClienteDto dto = new ClienteDto();
        dto.setIdCliente(cliente.getIdCliente());
        dto.setFechaAlta(cliente.getFechaAlta());
        dto.setNombre(cliente.getNombre());
        dto.setApellidos(cliente.getApellidos());
        dto.setFechaNacimiento(cliente.getFechaNacimiento());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setDireccion(cliente.getDireccion());
        dto.setNotasPrivadas(cliente.getNotasPrivadas());
        return dto;
    }
    /**
     * Convierte un ClienteRequestDto (petición) a una entidad Cliente (para guardar)
     */
    public Cliente toCliente(ClienteRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        Cliente cliente = new Cliente();
        // id y fechaAlta se generan solos
        cliente.setNombre(requestDto.getNombre());
        cliente.setApellidos(requestDto.getApellidos());
        cliente.setFechaNacimiento(requestDto.getFechaNacimiento());
        cliente.setTelefono(requestDto.getTelefono());
        cliente.setEmail(requestDto.getEmail());
        cliente.setDireccion(requestDto.getDireccion());
        cliente.setNotasPrivadas(requestDto.getNotasPrivadas());
        return cliente;
    }

    /**
     * Actualiza un entidad Cliente existente con datos de un DTO
     */
    public void updateClienteFromDto(ClienteRequestDto requestDto, Cliente cliente) {
        if (requestDto == null || cliente == null) {
            return;
        }
        cliente.setNombre(requestDto.getNombre());
        cliente.setApellidos(requestDto.getApellidos());
        cliente.setFechaNacimiento(requestDto.getFechaNacimiento());
        cliente.setTelefono(requestDto.getTelefono());
        cliente.setEmail(requestDto.getEmail());
        cliente.setDireccion(requestDto.getDireccion());
        cliente.setNotasPrivadas(requestDto.getNotasPrivadas());
    }
}
