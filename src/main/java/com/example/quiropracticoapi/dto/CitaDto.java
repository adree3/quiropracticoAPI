package com.example.quiropracticoapi.dto;

import com.example.quiropracticoapi.model.enums.EstadoCita;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CitaDto {
    private Integer idCita;
    private Integer idCliente;
    private String nombreClienteCompleto;
    private Integer idQuiropractico;
    private String nombreQuiropractico;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
    private String notasRecepcion;
}
