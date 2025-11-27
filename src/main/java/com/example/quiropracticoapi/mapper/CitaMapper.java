package com.example.quiropracticoapi.mapper;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

    public CitaDto toDto(Cita cita) {
        if (cita == null) return null;

        CitaDto dto = new CitaDto();
        dto.setIdCita(cita.getIdCita());
        dto.setIdCliente(cita.getCliente().getIdCliente());
        dto.setNombreClienteCompleto(cita.getCliente().getNombre() + " " + cita.getCliente().getApellidos());
        dto.setTelefonoCliente(cita.getCliente().getTelefono());

        dto.setIdQuiropractico(cita.getQuiropractico().getIdUsuario());
        dto.setNombreQuiropractico(cita.getQuiropractico().getNombreCompleto());

        dto.setFechaHoraInicio(cita.getFechaHoraInicio());
        dto.setFechaHoraFin(cita.getFechaHoraFin());
        dto.setEstado(cita.getEstado());
        dto.setNotasRecepcion(cita.getNotasRecepcion());

        return dto;
    }

    public Cita toEntity(CitaRequestDto dto, Cliente cliente, Usuario quiropractico) {
        if (dto == null) return null;

        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setQuiropractico(quiropractico);
        cita.setFechaHoraInicio(dto.getFechaHoraInicio());
        cita.setFechaHoraFin(dto.getFechaHoraFin());
        cita.setNotasRecepcion(dto.getNotasRecepcion());
        cita.setEstado(EstadoCita.programada);

        return cita;
    }
}
